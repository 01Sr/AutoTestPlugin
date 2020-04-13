import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;

/**
 * @author cb
 * @date 2019/10/31
 */
public class Util {
    public static final Key<String> locatorFileNameKey = Key.create("locatorFileName");
    public static final Key<String> locatorIdKey = Key.create("locatorId");
    public static final Key<String> locatorTypeKey = Key.create("locatorType");
    public static final Key<String> locatorValueKey = Key.create("locatorValue");


    @Nullable
    public static VirtualFile createFile(@NotNull String path) {
        File file = new File(path);
        if(file.exists()) {
            return LocalFileSystem.getInstance().findFileByPath(path);
        } else {
            File dir = file.getParentFile();
            if(!dir.exists() || !dir.isDirectory()) {
                dir.mkdirs();
            }
            try {
                file.createNewFile();
            } catch (IOException e) {
                return null;
            }
        }
        return LocalFileSystem.getInstance().refreshAndFindFileByPath(path);
    }

    public static void setLocatorData(UserDataHolderBase element, String fileName, String id, String type, String value) {
        element.putUserData(locatorFileNameKey, fileName);
        element.putUserData(locatorIdKey, id);
        element.putUserData(locatorTypeKey, type);
        element.putUserData(locatorValueKey, value);
    }

    public static String getLocatorFileName(UserDataHolderBase element) {
        return element.getUserData(locatorFileNameKey);
    }

    public static String getLocatorId(UserDataHolderBase element) {
        return element.getUserData(locatorIdKey);
    }

    public static String getLocatorType(UserDataHolderBase element) {
        return element.getUserData(locatorTypeKey);
    }

    public static String getLocatorValue(UserDataHolderBase element) {
        return element.getUserData(locatorValueKey);
    }
}
