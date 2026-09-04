/*
 * Copyright 2026 Graham Lynch
 * SPDX-License-Identifier: Apache-2.0
 */
package io.github.glynch.jscene3d.doom;

import static io.github.glynch.jscene3d.doom.TestDoomWadFiles.shorts;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.glynch.jscene3d.doom.diagnostic.DoomDiagnosticCode;
import io.github.glynch.jscene3d.doom.map.DoomMap;
import io.github.glynch.jscene3d.doom.map.DoomMapDecodeResult;
import io.github.glynch.jscene3d.doom.map.DoomMapDecoder;
import io.github.glynch.jscene3d.wad.WadArchive;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Specifies classic Doom map discovery and decoding through its public interface. */
final class DoomMapDecoderTest {
    @TempDir
    private Path temporaryDirectory;

    /** Discovers conventional Doom and Doom II marker names once in directory order. */
    @Test
    void discoversConventionalMapMarkers() throws IOException {
        List<TestDoomWadFiles.LumpContent> lumps = List.of(
                new TestDoomWadFiles.LumpContent("MAP01", new byte[0]),
                new TestDoomWadFiles.LumpContent("E1M1", new byte[0]),
                new TestDoomWadFiles.LumpContent("NOTAMAP", new byte[0]),
                new TestDoomWadFiles.LumpContent("MAP01", new byte[0]));
        WadArchive archive = write(lumps);

        assertThat(new DoomMapDecoder().discover(archive)).containsExactly("MAP01", "E1M1");
    }

    /** Decodes each classic map lump into immutable renderer-independent values. */
    @Test
    void decodesClassicMap() throws IOException {
        DoomMapDecodeResult result = new DoomMapDecoder().decode(write(TestDoomWadFiles.validMap("MAP01")), "map01");

        assertThat(result.isValid()).isTrue();
        assertThat(result.diagnostics()).isEmpty();
        DoomMap map = result.map().orElseThrow();
        assertThat(map.name()).isEqualTo("MAP01");
        assertThat(map.things()).containsExactly(new DoomMap.Thing(64, -32, 90, 1, 7));
        assertThat(map.linedefs()).containsExactly(new DoomMap.Linedef(0, 1, 1, 0, 0, 0, -1));
        assertThat(map.sidedefs()).containsExactly(new DoomMap.Sidedef(8, -4, "UPPER", "-", "MIDDLE", 0));
        assertThat(map.vertices()).containsExactly(new DoomMap.Vertex(0, 0), new DoomMap.Vertex(128, 0));
        assertThat(map.segs()).containsExactly(new DoomMap.Seg(0, 1, 0, 0, 0, 0));
        assertThat(map.subsectors()).containsExactly(new DoomMap.Subsector(1, 0));
        assertThat(map.nodes()).isEmpty();
        assertThat(map.sectors()).containsExactly(new DoomMap.Sector(0, 128, "FLOOR0_1", "CEIL1_1", 160, 0, 0));
        assertThat(map.rejectBytes()).containsExactly(0);
        assertThat(map.blockmap()).isEqualTo(new DoomMap.Blockmap(0, 0, 1, 1, List.of(List.of(0))));
        assertThat(map.blockmap().cell(0, 0)).containsExactly(0);
    }

    /** Rejects blockmap coordinates outside the decoded grid. */
    @Test
    void rejectsBlockmapCoordinatesOutsideGrid() throws IOException {
        DoomMap map = new DoomMapDecoder()
                .decode(write(TestDoomWadFiles.validMap("MAP01")), "MAP01")
                .map()
                .orElseThrow();
        DoomMap.Blockmap blockmap = map.blockmap();

        assertThatThrownBy(() -> blockmap.cell(1, 0))
                .isInstanceOf(IndexOutOfBoundsException.class)
                .hasMessage("blockmap cell is outside the grid");
    }

    /** Reports a missing marker using a stable typed diagnostic. */
    @Test
    void reportsMissingMap() throws IOException {
        DoomMapDecodeResult result = new DoomMapDecoder().decode(write(List.of()), "MAP01");

        assertDiagnostic(result, DoomDiagnosticCode.MAP_MISSING, "/maps/MAP01");
        assertThat(result.diagnostics().getFirst().details()).containsEntry("map", "MAP01");
    }

    /** Reports a stable lump-level diagnostic when fixed-size records are truncated. */
    @Test
    void rejectsTruncatedThingRecord() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("THINGS", new byte[] {1});

        assertDiagnostic(result, DoomDiagnosticCode.MAP_RECORD_SIZE_INVALID, "/maps/MAP01/THINGS");
        assertThat(result.diagnostics().getFirst().details())
                .containsEntry("size", "1")
                .containsEntry("recordSize", "10");
    }

    /** Rejects geometry indexes that cannot resolve inside the decoded map. */
    @Test
    void rejectsLinedefWithMissingVertex() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("LINEDEFS", shorts(0, 2, 1, 0, 0, 0, 0xffff));

        assertDiagnostic(result, DoomDiagnosticCode.MAP_REFERENCE_INVALID, "/maps/MAP01/LINEDEFS/0/endVertex");
    }

    /** Rejects a linedef that points outside the sidedef table. */
    @Test
    void rejectsLinedefWithMissingSidedef() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("LINEDEFS", shorts(0, 1, 1, 0, 0, 1, 0xffff));

        assertDiagnostic(result, DoomDiagnosticCode.MAP_REFERENCE_INVALID, "/maps/MAP01/LINEDEFS/0/rightSidedef");
    }

    /** Rejects a sidedef that points outside the sector table. */
    @Test
    void rejectsSidedefWithMissingSector() throws IOException {
        byte[] sidedef = TestDoomWadFiles.sidedef(8, -4, "UPPER", "-", "MIDDLE", 1);
        DoomMapDecodeResult result = decodeReplacing("SIDEDEFS", sidedef);

        assertDiagnostic(result, DoomDiagnosticCode.MAP_REFERENCE_INVALID, "/maps/MAP01/SIDEDEFS/0/sector");
    }

    /** Rejects a BSP segment that points outside the linedef table. */
    @Test
    void rejectsSegWithMissingLinedef() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("SEGS", shorts(0, 1, 0, 1, 0, 0));

        assertDiagnostic(result, DoomDiagnosticCode.MAP_REFERENCE_INVALID, "/maps/MAP01/SEGS/0/linedef");
    }

    /** Rejects a BSP segment direction outside zero or one. */
    @Test
    void rejectsInvalidSegDirection() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("SEGS", shorts(0, 1, 0, 0, 2, 0));

        assertDiagnostic(result, DoomDiagnosticCode.MAP_VALUE_INVALID, "/maps/MAP01/SEGS/0/direction");
    }

    /** Rejects a subsector whose contiguous seg range exceeds the seg table. */
    @Test
    void rejectsSubsectorWithMissingSegs() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("SSECTORS", shorts(2, 0));

        assertDiagnostic(result, DoomDiagnosticCode.MAP_REFERENCE_INVALID, "/maps/MAP01/SSECTORS/0/segs");
    }

    /** Rejects a BSP node child that points outside the subsector table. */
    @Test
    void rejectsNodeWithMissingSubsector() throws IOException {
        byte[] node = shorts(0, 0, 1, 0, 10, -10, -10, 10, 10, -10, -10, 10, 0x8001, 0x8000);
        DoomMapDecodeResult result = decodeReplacing("NODES", node);

        assertDiagnostic(result, DoomDiagnosticCode.MAP_REFERENCE_INVALID, "/maps/MAP01/NODES/0/rightChild");
    }

    /** Reports an invalid block-list offset at its owning blockmap cell. */
    @Test
    void rejectsBlockmapOffsetOutsideLump() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("BLOCKMAP", shorts(0, 0, 1, 1, 100));

        assertDiagnostic(result, DoomDiagnosticCode.MAP_BLOCKMAP_INVALID, "/maps/MAP01/BLOCKMAP/cells/0");
    }

    /** Rejects a blockmap cell that points outside the linedef table. */
    @Test
    void rejectsBlockmapCellWithMissingLinedef() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("BLOCKMAP", shorts(0, 0, 1, 1, 5, 0, 1, 0xffff));

        assertDiagnostic(result, DoomDiagnosticCode.MAP_REFERENCE_INVALID, "/maps/MAP01/BLOCKMAP/cells/0/0");
    }

    /** Rejects a blockmap cell without its terminating sentinel. */
    @Test
    void rejectsUnterminatedBlockmapCell() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("BLOCKMAP", shorts(0, 0, 1, 1, 5, 0, 0));

        assertDiagnostic(result, DoomDiagnosticCode.MAP_BLOCKMAP_INVALID, "/maps/MAP01/BLOCKMAP/cells/0");
    }

    /** Rejects a REJECT table too short to contain one bit per sector pair. */
    @Test
    void rejectsTruncatedRejectTable() throws IOException {
        DoomMapDecodeResult result = decodeReplacing("REJECT", new byte[0]);

        assertDiagnostic(result, DoomDiagnosticCode.MAP_REJECT_SIZE_INVALID, "/maps/MAP01/REJECT");
    }

    /** Distinguishes unsupported UDMF maps from corrupt classic lump ordering. */
    @Test
    void rejectsUdmfMapExplicitly() throws IOException {
        List<TestDoomWadFiles.LumpContent> lumps = List.of(
                new TestDoomWadFiles.LumpContent("MAP01", new byte[0]),
                new TestDoomWadFiles.LumpContent(
                        "TEXTMAP", "namespace=\"zdoom\";".getBytes(StandardCharsets.US_ASCII)));
        DoomMapDecodeResult result = new DoomMapDecoder().decode(write(lumps), "MAP01");

        assertDiagnostic(result, DoomDiagnosticCode.MAP_FORMAT_UDMF_UNSUPPORTED, "/maps/MAP01/TEXTMAP");
    }

    /** Detects the BEHAVIOR marker used by unsupported Hexen-format maps. */
    @Test
    void rejectsHexenFormatMapExplicitly() throws IOException {
        List<TestDoomWadFiles.LumpContent> lumps = TestDoomWadFiles.validMap("MAP01");
        lumps.add(new TestDoomWadFiles.LumpContent("BEHAVIOR", new byte[] {0, 0, 0, 0}));
        DoomMapDecodeResult result = new DoomMapDecoder().decode(write(lumps), "MAP01");

        assertDiagnostic(result, DoomDiagnosticCode.MAP_FORMAT_HEXEN_UNSUPPORTED, "/maps/MAP01/BEHAVIOR");
    }

    /** Reports the first unexpected lump in an incomplete classic sequence. */
    @Test
    void rejectsInvalidClassicLayout() throws IOException {
        List<TestDoomWadFiles.LumpContent> lumps = TestDoomWadFiles.validMap("MAP01");
        lumps.set(2, new TestDoomWadFiles.LumpContent("WRONG", new byte[0]));
        DoomMapDecodeResult result = new DoomMapDecoder().decode(write(lumps), "MAP01");

        assertDiagnostic(result, DoomDiagnosticCode.MAP_LAYOUT_INVALID, "/maps/MAP01/LINEDEFS");
        assertThat(result.diagnostics().getFirst().details())
                .containsEntry("expectedLump", "LINEDEFS")
                .containsEntry("actualLump", "WRONG");
    }

    /** Decodes one complete map after replacing a named map lump. */
    private DoomMapDecodeResult decodeReplacing(String name, byte[] content) throws IOException {
        return new DoomMapDecoder().decode(write(TestDoomWadFiles.validMapReplacing(name, content)), "MAP01");
    }

    /** Writes and loads one test archive at a test-owned temporary path. */
    private WadArchive write(List<TestDoomWadFiles.LumpContent> lumps) throws IOException {
        return TestDoomWadFiles.writeAndLoad(temporaryDirectory.resolve("map.wad"), lumps);
    }

    /** Asserts the common shape of one terminal decoding diagnostic. */
    private static void assertDiagnostic(DoomMapDecodeResult result, DoomDiagnosticCode code, String location) {
        assertThat(result.map()).isEmpty();
        assertThat(result.isValid()).isFalse();
        assertThat(result.diagnostics()).singleElement().satisfies(diagnostic -> {
            assertThat(diagnostic.code()).isEqualTo(code);
            assertThat(diagnostic.message()).isEqualTo(code.defaultMessage());
            assertThat(diagnostic.location()).isEqualTo(location);
        });
    }
}
