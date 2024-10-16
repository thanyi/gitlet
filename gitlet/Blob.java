package gitlet;

import java.io.File;
import java.io.Serializable;

import static gitlet.Refs.*;
import static gitlet.Repository.*;
import static gitlet.Utils.*;

public class Blob implements Serializable {
    private String content;
    private File filePath;
    private String hashName;

    public Blob(String content, String hashName) {
        this.content = content;
        this.hashName = hashName;
        this.filePath = join(BLOBS_FOLDER, hashName);
    }


    public File getFilePath() {
        return filePath;
    }



    /**
     * 将blob对象保存进 BLOB_FOLDER文件，内容就是blob文件的content
     */
    public void saveBlob() {
        if (!filePath.exists()) {
            // 如果这个blob原先不存在，则进行blob的储存
            writeContents(filePath, this.content);
        }

    }



    /**
     * 根据blobName获取到Blob的内容，其中blobName是一个hash值
     * 若是没有这个文件，返回null
     *
     * @return Blob的内容
     */
    public static String getBlobContentFromName(String blobName) {
        /* 获取commit文件 */
        String blobContent = null;
        File blobFile = join(BLOBS_FOLDER, blobName);
        if (blobFile.isFile() && blobFile.exists()) {
            blobContent = readContentsAsString(blobFile);
        }


        return blobContent;

    }

    /**
     * 将blob.content中的内容覆盖进file文件中
     */
    public static void overWriteFileWithBlob(File file, String content) {
        writeContents(file, content);
    }

}

