package org.example.myblog.serverl;

import org.example.myblog.dto.FavoriteFolderDTO;

import java.util.List;

public interface FavoriteFolderService {

    List<FavoriteFolderDTO> listFolders(Long userId);

    /** 新建收藏夹，返回新 id */
    long createFolder(Long userId, String name);

    /** 删除非默认收藏夹，夹内收藏移至默认夹 */
    void deleteFolder(Long userId, Long folderId);

    /** 不存在时创建「默认收藏夹」 */
    long getOrCreateDefaultFolderId(Long userId);

    /** 校验收藏夹属于该用户，否则抛 IllegalArgumentException */
    void assertFolderOwnedByUser(Long userId, Long folderId);

    boolean isFolderOwnedByUser(Long userId, Long folderId);
}
