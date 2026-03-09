package app.nook.book.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    String uploadCover(MultipartFile file);
    String uploadProfile(MultipartFile file);


    void deleteCoverByUrl(String fileUrl);
    void deleteProfileByUrl(String fileUrl);
}
