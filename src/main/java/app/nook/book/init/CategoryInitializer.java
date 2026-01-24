package app.nook.book.init;

import app.nook.book.entity.BookCategory;
import app.nook.book.entity.Category;
import app.nook.book.entity.MallType;
import app.nook.book.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class CategoryInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    @Override
    public void run(ApplicationArguments args) {
        log.info("[CATEGORY INIT] 카테고리 동기화 작업을 시작합니다...");

        // 1. 중복 방지용 키 (Type_Name)
        Set<String> existingKeys = categoryRepository.findAll().stream()
                .map(c -> c.getMallType() + "_" + c.getCategoryName())
                .collect(Collectors.toSet());

        List<Category> toSave = new ArrayList<>();

        for (BookCategory bc : BookCategory.values()) {
            // (1) BOOK 타입 저장 (ID: bookId 사용)
            addIfMissing(toSave, existingKeys, MallType.BOOK, bc.getDbName(), bc.getBookId());

            // (2) EBOOK 타입 저장 (ID: ebookId 사용)
            addIfMissing(toSave, existingKeys, MallType.EBOOK, bc.getDbName(), bc.getEbookId());
        }

        if (!toSave.isEmpty()) {
            categoryRepository.saveAll(toSave);
            log.info("[CATEGORY INIT] 총 {}개의 카테고리가 추가되었습니다.", toSave.size());
        }
    }

    private void addIfMissing(List<Category> list, Set<String> existing, MallType type, String name, int id) {
        if (!existing.contains(type + "_" + name)) {
            list.add(Category.builder()
                    .mallType(type)
                    .categoryName(name)
                    .aladinCategoryId(id)
                    .build());
        }
    }
}
