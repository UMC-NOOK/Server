package app.nook.r2.policy;

import app.nook.global.config.R2Properties;
import app.nook.global.exception.CustomException;
import app.nook.global.response.FileErrorCode;

import java.util.Arrays;

public enum ImageStoragePolicy {

    BOOK("book", true),
    PROFILE("profile", true),
    RECORD("record", false);

    private final String uploadType;
    private final boolean publiclyReadable;

    ImageStoragePolicy(String uploadType, boolean publiclyReadable) {
        this.uploadType = uploadType;
        this.publiclyReadable = publiclyReadable;
    }

    public static ImageStoragePolicy fromUploadType(String uploadType) {
        return Arrays.stream(values())
                .filter(policy -> policy.uploadType.equals(uploadType))
                .findFirst()
                .orElseThrow(() -> new CustomException(FileErrorCode.INVALID_FILE_TYPE));
    }

    public static ImageStoragePolicy fromKey(String key) {
        if (key == null) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }
        int separatorIndex = key.indexOf('/');
        if (separatorIndex <= 0) {
            throw new CustomException(FileErrorCode.INVALID_FILE_TYPE);
        }
        return fromUploadType(key.substring(0, separatorIndex));
    }

    public String bucketName(R2Properties r2) {
        return publiclyReadable ? r2.publicBucketName() : r2.privateBucketName();
    }

    public String uploadType() {
        return uploadType;
    }

    public String prefix() {
        return uploadType + "/";
    }

    public boolean isPubliclyReadable() {
        return publiclyReadable;
    }
}
