package com.roadscanner.dao.upload;

import com.roadscanner.domain.upload.FileUploadVO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:/dao-test-context.xml")
@Transactional
public class FileUploadDaoImplTest {

    @Autowired
    private FileUploadDao dao;

    private FileUploadVO upload1;
    private FileUploadVO upload2;
    private FileUploadVO upload3;

    @Before
    public void setUp() {
        upload1 = upload("dao-dog.jpg", 10, "https://example.test/dog.jpg", 700);
        upload2 = upload("dao-cat.jpg", 10, "https://example.test/cat.jpg", 800);
        upload3 = upload("dao-cow.jpg", 20, "https://example.test/cow.jpg", 900);
    }

    @Test
    public void savesAndSelectsByNameAndIndex() throws Exception {
        assertThat(dao.doSave(upload1)).isEqualTo(1);

        FileUploadVO byName = dao.doSelectOne(keyByName(upload1.getName()));
        assertUpload(byName, upload1);
        assertThat(byName.getIdx()).isPositive();
        assertThat(byName.getChecked()).isZero();

        FileUploadVO keyByIndex = new FileUploadVO();
        keyByIndex.setIdx(byName.getIdx());
        assertUpload(dao.doSelectOne(keyByIndex), upload1);
    }

    @Test
    public void updatesUploadReviewFields() throws Exception {
        dao.doSave(upload1);
        FileUploadVO stored = dao.doSelectOne(keyByName(upload1.getName()));
        stored.setUrl("https://example.test/updated.jpg");
        stored.setCategory(30);
        stored.setChecked(1);
        stored.setU1(1);
        stored.setU2(1);
        stored.setU3(1);

        assertThat(dao.doUpdate(stored)).isEqualTo(1);

        FileUploadVO updated = dao.doSelectOne(keyByName(upload1.getName()));
        assertThat(updated.getUrl()).isEqualTo("https://example.test/updated.jpg");
        assertThat(updated.getCategory()).isEqualTo(30);
        assertThat(updated.getChecked()).isEqualTo(1);
        assertThat(updated.getU1()).isEqualTo(1);
        assertThat(updated.getU2()).isEqualTo(1);
        assertThat(updated.getU3()).isEqualTo(1);
    }

    @Test
    public void deletesUpload() throws Exception {
        dao.doSave(upload1);

        assertThat(dao.doDelete(keyByName(upload1.getName()))).isEqualTo(1);
        assertThat(dao.doSelectOne(keyByName(upload1.getName()))).isNull();
    }

    @Test
    public void retrievesUncheckedUploadsWithPaging() throws Exception {
        saveAll();
        FileUploadVO search = new FileUploadVO();
        search.setPageNo(1);
        search.setPageSize(2);

        List<FileUploadVO> page = dao.doRetrieve(search);

        assertThat(page).hasSize(2);
        assertThat(page).allSatisfy(item -> {
            assertThat(item.getIdx()).isPositive();
            assertThat(item.getTotalCnt()).isEqualTo(3);
        });
        assertThat(page).extracting(FileUploadVO::getIdx).doesNotHaveDuplicates();
    }

    @Test
    public void retrievesUncheckedUploadsByCategory() throws Exception {
        saveAll();
        FileUploadVO search = new FileUploadVO();
        search.setCategory(10);
        search.setPageNo(1);
        search.setPageSize(10);

        List<FileUploadVO> page = dao.doRetrieveByCategory(search);

        assertThat(page).extracting(FileUploadVO::getName)
                .containsExactlyInAnyOrder(upload1.getName(), upload2.getName());
        assertThat(page).allSatisfy(item -> {
            assertThat(item.getIdx()).isPositive();
            assertThat(item.getCategory()).isEqualTo(10);
            assertThat(item.getTotalCnt()).isEqualTo(2);
        });
    }

    @Test
    public void excludesReviewedUploadsFromRetrieval() throws Exception {
        saveAll();
        FileUploadVO reviewed = dao.doSelectOne(keyByName(upload1.getName()));
        reviewed.setChecked(1);
        dao.doUpdate(reviewed);

        FileUploadVO search = new FileUploadVO();
        search.setPageNo(1);
        search.setPageSize(10);

        assertThat(dao.doRetrieve(search)).extracting(FileUploadVO::getName)
                .doesNotContain(upload1.getName());
    }

    @Test
    public void findsOnlyBoundedPendingDeletes() throws Exception {
        saveAll();
        setChecked(upload1.getName(), -1);
        setChecked(upload2.getName(), -2);

        List<FileUploadVO> firstBatch = dao.findPendingDeletes(1);
        List<FileUploadVO> allPending = dao.findPendingDeletes(10);

        assertThat(firstBatch).hasSize(1);
        assertThat(allPending).hasSize(2);
        assertThat(allPending).extracting(FileUploadVO::getName)
                .containsExactlyInAnyOrder(upload1.getName(), upload2.getName());
        assertThat(allPending).extracting(FileUploadVO::getChecked)
                .containsExactlyInAnyOrder(-1, -2);
    }

    @Test
    public void restoresPendingDeleteOnlyFromExpectedState() throws Exception {
        dao.doSave(upload1);
        FileUploadVO stored = dao.doSelectOne(keyByName(upload1.getName()));
        stored.setChecked(-1);
        dao.doUpdate(stored);

        assertThat(dao.restorePendingDelete(stored.getIdx(), stored.getName(), -2, 1))
                .isZero();
        assertThat(dao.restorePendingDelete(stored.getIdx(), stored.getName(), -1, 0))
                .isEqualTo(1);
        assertThat(dao.doSelectOne(keyByName(stored.getName())).getChecked()).isZero();
    }

    @Test
    public void aggregatesMonthlyFeedback() throws Exception {
        saveAllWithFeedback();
        FileUploadVO search = new FileUploadVO();
        search.setUploadDate(LocalDate.now().toString());

        List<FileUploadVO> feedback = dao.monthlyFeedback(search);

        assertThat(feedback).hasSize(1);
        assertThat(feedback.get(0).getU1()).isEqualTo(1);
        assertThat(feedback.get(0).getU2()).isEqualTo(1);
        assertThat(feedback.get(0).getU3()).isEqualTo(1);
    }

    @Test
    public void aggregatesTotalFeedback() throws Exception {
        saveAllWithFeedback();

        FileUploadVO totals = dao.totalFeedback(new FileUploadVO());

        assertThat(totals).isNotNull();
        assertThat(totals.getU1()).isEqualTo(1);
        assertThat(totals.getU2()).isEqualTo(1);
        assertThat(totals.getU3()).isEqualTo(1);
    }

    @Test
    public void feedbackAggregatesIgnoreFlagsOutsideNegativeFeedback() throws Exception {
        saveAll();
        setFeedback(upload1.getName(), 1, 0, 0);
        setFeedback(upload3.getName(), 0, 1, 1);

        FileUploadVO totals = dao.totalFeedback(new FileUploadVO());

        assertThat(totals).isNotNull();
        assertThat(totals.getU1()).isZero();
        assertThat(totals.getU2()).isZero();
        assertThat(totals.getU3()).isZero();
    }

    private FileUploadVO upload(String name, int category, String url, double size) {
        return new FileUploadVO(0, "member01", category, null, name, url, size, 0, 0, 0, 0);
    }

    private FileUploadVO keyByName(String name) {
        FileUploadVO key = new FileUploadVO();
        key.setName(name);
        return key;
    }

    private void saveAll() throws Exception {
        dao.doSave(upload1);
        dao.doSave(upload2);
        dao.doSave(upload3);
    }

    private void saveAllWithFeedback() throws Exception {
        saveAll();
        setNegativeFeedback(upload1.getName(), 1, 0, 0);
        setNegativeFeedback(upload2.getName(), 0, 1, 0);
        setNegativeFeedback(upload3.getName(), 0, 0, 1);
    }

    private void setNegativeFeedback(String name, int u1, int u2, int u3) throws Exception {
        FileUploadVO stored = dao.doSelectOne(keyByName(name));
        stored.setCategory(30);
        stored.setU1(u1);
        stored.setU2(u2);
        stored.setU3(u3);
        dao.doUpdate(stored);
    }

    private void setFeedback(String name, int u1, int u2, int u3) throws Exception {
        FileUploadVO stored = dao.doSelectOne(keyByName(name));
        stored.setU1(u1);
        stored.setU2(u2);
        stored.setU3(u3);
        dao.doUpdate(stored);
    }

    private void setChecked(String name, int checked) throws Exception {
        FileUploadVO stored = dao.doSelectOne(keyByName(name));
        stored.setChecked(checked);
        dao.doUpdate(stored);
    }

    private void assertUpload(FileUploadVO actual, FileUploadVO expected) {
        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(expected.getId());
        assertThat(actual.getCategory()).isEqualTo(expected.getCategory());
        assertThat(actual.getName()).isEqualTo(expected.getName());
        assertThat(actual.getUrl()).isEqualTo(expected.getUrl());
        assertThat(actual.getFileSize()).isEqualTo(expected.getFileSize());
    }
}
