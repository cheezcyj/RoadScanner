package com.roadscanner.controller.upload;

import com.roadscanner.domain.upload.FileUploadVO;
import com.roadscanner.service.upload.FileUploadService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.ui.ExtendedModelMap;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;

@RunWith(MockitoJUnitRunner.class)
public class ImgManageControllerTest {

    @Mock
    private FileUploadService fileUploadService;

    private ImgManageController controller;

    @Before
    public void setUp() {
        controller = new ImgManageController(fileUploadService);
    }

    @Test
    public void deleteUsesCanonicalStoredFileInsteadOfClientFields() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setIdx(42);
        request.setName("attacker-controlled-key.png");
        request.setChecked(1);

        FileUploadVO storedFile = new FileUploadVO();
        storedFile.setIdx(42);
        storedFile.setName("canonical-key.png");
        storedFile.setChecked(0);

        when(fileUploadService.doSelectOne(request)).thenReturn(storedFile);
        when(fileUploadService.doDelete(storedFile)).thenReturn(1);

        String response = controller.doDelete(request);

        assertThat(response).contains("\"msgId\":\"1\"");
        verify(fileUploadService).doSelectOne(request);
        verify(fileUploadService).doDelete(storedFile);
        verify(fileUploadService, never()).doDelete(request);
    }

    @Test
    public void deleteMissingFileReturnsNotFoundWithoutTouchingS3() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setName("missing.png");
        when(fileUploadService.doSelectOne(request)).thenReturn(null);

        assertThatThrownBy(() -> controller.doDelete(request))
                .hasMessageContaining("404 NOT_FOUND");

        verify(fileUploadService).doSelectOne(request);
        verify(fileUploadService, never()).doDelete(org.mockito.ArgumentMatchers.any(FileUploadVO.class));
    }

    @Test
    public void updateMissingFileReturnsNotFoundWithoutChangingAnything() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setName("missing.png");
        when(fileUploadService.doSelectOne(request)).thenReturn(null);

        assertThatThrownBy(() -> controller.checkedUpdate(request))
                .hasMessageContaining("404 NOT_FOUND");

        verify(fileUploadService, never()).checkedUpdate(any(FileUploadVO.class));
    }

    @Test
    public void selectMissingFileReturnsNotFound() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setName("missing.png");
        when(fileUploadService.doSelectOne(request)).thenReturn(null);

        assertThatThrownBy(() -> controller.doSelectOne(request))
                .hasMessageContaining("404 NOT_FOUND");
    }

    @Test
    public void emptyImageListStillExposesAValidFirstPage() throws Exception {
        FileUploadVO request = new FileUploadVO();
        ExtendedModelMap model = new ExtendedModelMap();
        when(fileUploadService.doRetrieve(request)).thenReturn(Collections.<FileUploadVO>emptyList());

        String viewName = controller.imgManagement(1, 0, request, model);

        assertThat(viewName).isEqualTo("imgManagement");
        assertThat(request.getPageNo()).isEqualTo(1);
        assertThat(request.getPageSize()).isEqualTo(9);
        assertThat(model.get("totalPages")).isEqualTo(1);
        assertThat(model.get("startPage")).isEqualTo(1);
        assertThat(model.get("endPage")).isEqualTo(1);
        assertThat(model.get("prevBlock")).isEqualTo(1);
        assertThat(model.get("nextBlock")).isEqualTo(1);
    }

    @Test
    public void imageListClampsOversizedPageAndForcesServerPageSize() throws Exception {
        FileUploadVO request = new FileUploadVO();
        request.setPageSize(9999);
        FileUploadVO firstPageRow = new FileUploadVO();
        firstPageRow.setTotalCnt(10);
        FileUploadVO lastPageRow = new FileUploadVO();
        lastPageRow.setTotalCnt(10);
        doReturn(Collections.<FileUploadVO>emptyList())
                .doReturn(Collections.singletonList(firstPageRow))
                .doReturn(Collections.singletonList(lastPageRow))
                .when(fileUploadService).doRetrieve(request);
        ExtendedModelMap model = new ExtendedModelMap();

        controller.imgManagement(999, 999, request, model);

        assertThat(request.getPageNo()).isEqualTo(2);
        assertThat(request.getPageSize()).isEqualTo(9);
        assertThat(request.getCategory()).isEqualTo(0);
        assertThat(model.get("pageNo")).isEqualTo(2);
        assertThat(model.get("totalPages")).isEqualTo(2);
        assertThat(model.get("list")).isEqualTo(Collections.singletonList(lastPageRow));
        verify(fileUploadService, times(3)).doRetrieve(request);
    }

    @Test
    public void bulkUpdateRejectsEmptyOrMissingSelections() throws Exception {
        assertThat(controller.checkedUpdateMultiple(null)).isEqualTo(0);
        assertThat(controller.checkedUpdateMultiple(new String[0])).isEqualTo(0);
        assertThat(controller.checkedUpdateMultiple(new String[] { "missing.png" })).isEqualTo(0);

        verify(fileUploadService, never()).checkedUpdate(any(FileUploadVO.class));
    }

    @Test
    public void bulkUpdateRejectsBlankAndOversizedSelectionsBeforeLookup() throws Exception {
        assertThat(controller.checkedUpdateMultiple(new String[] { "   " })).isEqualTo(0);
        assertThat(controller.checkedUpdateMultiple(new String[101])).isEqualTo(0);

        verify(fileUploadService, never()).doSelectOne(any(FileUploadVO.class));
        verify(fileUploadService, never()).checkedUpdate(any(FileUploadVO.class));
    }

    @Test
    public void bulkUpdateDeduplicatesNamesAndUsesStoredMetadata() throws Exception {
        FileUploadVO stored = new FileUploadVO();
        stored.setName("stored.png");
        when(fileUploadService.doSelectOne(any(FileUploadVO.class))).thenReturn(stored);
        when(fileUploadService.checkedUpdate(stored)).thenReturn(1);

        int result = controller.checkedUpdateMultiple(
                new String[] { "stored.png", "stored.png" });

        assertThat(result).isEqualTo(1);
        verify(fileUploadService, times(1)).doSelectOne(any(FileUploadVO.class));
        verify(fileUploadService).checkedUpdate(stored);
    }

    @Test
    public void bulkDeleteDoesNotModifyAnythingWhenAStoredFileIsMissing() throws Exception {
        FileUploadVO stored = new FileUploadVO();
        stored.setName("first.png");
        doReturn(stored)
                .doReturn(null)
                .when(fileUploadService).doSelectOne(any(FileUploadVO.class));
        int result = controller.doDeleteMultiple(
                new String[] { "first.png", "missing.png" });

        assertThat(result).isEqualTo(0);
        verify(fileUploadService, never()).doDelete(any(FileUploadVO.class));
    }
}
