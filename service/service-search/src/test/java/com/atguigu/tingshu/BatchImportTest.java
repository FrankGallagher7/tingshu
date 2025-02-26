package com.atguigu.tingshu;

import com.atguigu.tingshu.search.service.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class BatchImportTest {

    @Autowired
    private SearchService searchService;

    @Test
    public void importAlbum() {

//        searchService.upperAlbum(1L);
        for (long i = 2; i < 1610; i++) {
            try {
                searchService.upperAlbum(i);
                System.out.println("导入专辑ID成功：" + i);
            } catch (Exception e) {
                continue;
            }
        }
    }
}
