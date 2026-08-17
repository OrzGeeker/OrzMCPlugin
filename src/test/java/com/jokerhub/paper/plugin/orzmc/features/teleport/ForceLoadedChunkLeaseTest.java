package com.jokerhub.paper.plugin.orzmc.features.teleport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.jokerhub.paper.plugin.orzmc.infra.server.RegionSchedulerProvider;
import com.jokerhub.paper.plugin.orzmc.testutil.ServiceTestBase;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ForceLoadedChunkLeaseTest extends ServiceTestBase {

    private World world;
    private Chunk chunk;

    /** 默认同步直跑（不投递），让既有断言在任务体内即时执行；投递目标用 captureProvider 单独验证。 */
    private RegionSchedulerProvider provider;

    private ForceLoadedChunkLease lease;

    @BeforeEach
    void setUp() {
        world = mock(World.class);
        chunk = mock(Chunk.class);
        when(world.getChunkAt(anyInt(), anyInt())).thenReturn(chunk);
        provider = (w, cx, cz, task) -> task.run();
        lease = new ForceLoadedChunkLease(provider);
    }

    /** 捕获每次投递的 (world, cx, cz, task) 并同步执行任务体的 provider。 */
    private RegionSchedulerProvider capturingProvider(List<Object[]> dispatches) {
        return (w, cx, cz, task) -> {
            dispatches.add(new Object[] {w, cx, cz, task});
            task.run();
        };
    }

    // ---- Folia 区域亲和：force-load 操作必须投递到目标 chunk 的 region ----

    @Test
    void acquire_dispatchesToOwningChunkRegion() {
        List<Object[]> dispatches = new ArrayList<>();
        lease = new ForceLoadedChunkLease(capturingProvider(dispatches));

        lease.acquire(world, 10, 7);

        assertEquals(1, dispatches.size());
        assertEquals(world, dispatches.get(0)[0]);
        assertEquals(10, dispatches.get(0)[1]);
        assertEquals(7, dispatches.get(0)[2]);
        // 任务体在投递后同步执行，force-load 仍生效
        verify(chunk).setForceLoaded(true);
    }

    @Test
    void release_dispatchesToOwningChunkRegion() {
        List<Object[]> dispatches = new ArrayList<>();
        lease = new ForceLoadedChunkLease(capturingProvider(dispatches));
        when(world.isChunkLoaded(1, 2)).thenReturn(true);
        lease.acquire(world, 1, 2);

        clearInvocations(world, chunk);
        dispatches.clear();
        lease.release(world, 1, 2);

        assertEquals(1, dispatches.size());
        assertEquals(world, dispatches.get(0)[0]);
        assertEquals(1, dispatches.get(0)[1]);
        assertEquals(2, dispatches.get(0)[2]);
        verify(chunk).setForceLoaded(false);
    }

    @Test
    void acquire_firstReference_loadsAndForces() {
        lease.acquire(world, 1, 2);

        verify(world).getChunkAt(1, 2);
        verify(chunk).setForceLoaded(true);
    }

    @Test
    void acquire_secondReference_doesNotReload() {
        lease.acquire(world, 1, 2);
        lease.acquire(world, 1, 2);

        verify(world, times(1)).getChunkAt(1, 2);
        verify(chunk, times(1)).setForceLoaded(true);
    }

    @Test
    void release_higherReference_doesNotUnload() {
        lease.acquire(world, 1, 2);
        lease.acquire(world, 1, 2);

        lease.release(world, 1, 2);

        verify(chunk, never()).setForceLoaded(false);
        verify(world, never()).unloadChunk(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void release_lastReference_unloadsWhenChunkLoaded() {
        lease.acquire(world, 1, 2);
        when(world.isChunkLoaded(1, 2)).thenReturn(true);

        lease.release(world, 1, 2);

        verify(world).isChunkLoaded(1, 2);
        verify(chunk).setForceLoaded(false);
        verify(world).unloadChunk(1, 2, true);
    }

    @Test
    void release_lastReference_skipsUnloadWhenPlayerInChunk() {
        lease.acquire(world, 1, 2);
        when(world.isChunkLoaded(1, 2)).thenReturn(true);

        Player player = mock(Player.class);
        Location loc = mock(Location.class);
        when(world.getPlayers()).thenReturn(List.of(player));
        when(player.getLocation()).thenReturn(loc);
        when(loc.getBlockX()).thenReturn(20); // chunk(1,2)：x 16..31
        when(loc.getBlockZ()).thenReturn(40); // z 32..47

        lease.release(world, 1, 2);

        // 解除 force-load，但区块内有玩家则不主动卸载。
        verify(chunk).setForceLoaded(false);
        verify(world, never()).unloadChunk(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void release_lastReference_skipsWhenChunkUnloaded() {
        lease.acquire(world, 1, 2);
        when(world.isChunkLoaded(1, 2)).thenReturn(false);

        clearInvocations(world, chunk);
        lease.release(world, 1, 2);

        // 已卸载时不得 getChunkAt 重新加载区块。
        verify(world, never()).getChunkAt(1, 2);
        verify(chunk, never()).setForceLoaded(false);
    }

    @Test
    void release_unknownReference_isNoOp() {
        lease.release(world, 3, 4);

        verify(world, never()).getChunkAt(anyInt(), anyInt());
        verify(world, never()).isChunkLoaded(anyInt(), anyInt());
    }

    @Test
    void otherOwnersForceLoad_notClearedOrUnloaded() {
        when(chunk.isForceLoaded()).thenReturn(true); // 其他插件已 force-load

        lease.acquire(world, 1, 2);
        verify(chunk, never()).setForceLoaded(true);

        when(world.isChunkLoaded(1, 2)).thenReturn(true);
        lease.release(world, 1, 2);

        // 不是我们 force-load 的：不解除、不主动卸载。
        verify(chunk, never()).setForceLoaded(false);
        verify(world, never()).unloadChunk(anyInt(), anyInt(), anyBoolean());
    }

    @Test
    void negativeCoordinates_trackedDistinctly() {
        lease.acquire(world, -1, -2);
        verify(chunk, times(1)).setForceLoaded(true);

        lease.acquire(world, -1, -2);
        verify(chunk, times(1)).setForceLoaded(true); // 第二次 acquire 不重复加载

        when(world.isChunkLoaded(-1, -2)).thenReturn(true);
        clearInvocations(world, chunk);

        lease.release(world, -1, -2);
        verify(chunk, never()).setForceLoaded(false);

        lease.release(world, -1, -2);
        verify(world, times(1)).getChunkAt(-1, -2);
        verify(chunk, times(1)).setForceLoaded(false);
    }

    @Test
    void sameChunkCoords_distinctWorlds_independent() {
        World worldB = mock(World.class);
        Chunk chunkB = mock(Chunk.class);
        when(worldB.getChunkAt(1, 2)).thenReturn(chunkB);

        lease.acquire(world, 1, 2);
        lease.acquire(worldB, 1, 2);

        when(world.isChunkLoaded(1, 2)).thenReturn(true);
        when(worldB.isChunkLoaded(1, 2)).thenReturn(true);

        lease.release(world, 1, 2);
        lease.release(worldB, 1, 2);

        verify(chunk).setForceLoaded(false);
        verify(chunkB).setForceLoaded(false);
    }
}
