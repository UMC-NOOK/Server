package app.nook.book.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    String uploadCover(MultipartFile file);
    void deleteByUrl(String fileUrl);
}
