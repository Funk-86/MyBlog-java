package org.example.myblog.serverl.impl;

import org.example.myblog.dto.FavoriteFolderDTO;
import org.example.myblog.entiy.FavoriteFolder;
import org.example.myblog.mapper.FavoriteFolderMapper;
import org.example.myblog.serverl.FavoriteFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FavoriteFolderServiceImpl implements FavoriteFolderService {

    private static final String DEFAULT_NAME = "默认收藏夹";
    private static final int MAX_NAME_LEN = 80;

    @Autowired
    private FavoriteFolderMapper favoriteFolderMapper;

    @Override
    public List<FavoriteFolderDTO> listFolders(Long userId) {
        if (userId == null) return List.of();
        return favoriteFolderMapper.listByUserId(userId);
    }

    @Override
    @Transactional
    public long createFolder(Long userId, String name) {
        if (userId == null) throw new IllegalArgumentException("userId required");
        String n = name == null ? "" : name.trim();
        if (n.isEmpty()) throw new IllegalArgumentException("收藏夹名称不能为空");
        if (n.length() > MAX_NAME_LEN) throw new IllegalArgumentException("名称过长");
        if (DEFAULT_NAME.equals(n)) throw new IllegalArgumentException("不能使用系统保留名称");
        if (favoriteFolderMapper.countByUserAndName(userId, n) > 0) {
            throw new IllegalArgumentException("已有同名收藏夹");
        }
        LocalDateTime now = LocalDateTime.now();
        FavoriteFolder f = new FavoriteFolder();
        f.setUserId(userId);
        f.setName(n);
        f.setIsDefault(0);
        f.setSortOrder(0);
        f.setCreatedAt(now);
        f.setUpdatedAt(now);
        favoriteFolderMapper.insert(f);
        return f.getId();
    }

    @Override
    @Transactional
    public void deleteFolder(Long userId, Long folderId) {
        if (userId == null || folderId == null) return;
        FavoriteFolder folder = favoriteFolderMapper.selectById(folderId);
        if (folder == null || !userId.equals(folder.getUserId())) return;
        if (folder.getIsDefault() != null && folder.getIsDefault() == 1) {
            throw new IllegalArgumentException("不能删除默认收藏夹");
        }
        favoriteFolderMapper.moveFavoritesToDefault(userId, folderId);
        favoriteFolderMapper.deleteNonDefault(folderId, userId);
    }

    @Override
    @Transactional
    public long getOrCreateDefaultFolderId(Long userId) {
        if (userId == null) throw new IllegalArgumentException("userId required");
        Long id = favoriteFolderMapper.selectDefaultFolderId(userId);
        if (id != null) return id;
        LocalDateTime now = LocalDateTime.now();
        FavoriteFolder f = new FavoriteFolder();
        f.setUserId(userId);
        f.setName(DEFAULT_NAME);
        f.setIsDefault(1);
        f.setSortOrder(0);
        f.setCreatedAt(now);
        f.setUpdatedAt(now);
        favoriteFolderMapper.insert(f);
        return f.getId();
    }

    @Override
    public void assertFolderOwnedByUser(Long userId, Long folderId) {
        if (userId == null || folderId == null) {
            throw new IllegalArgumentException("收藏夹参数无效");
        }
        FavoriteFolder folder = favoriteFolderMapper.selectById(folderId);
        if (folder == null || !userId.equals(folder.getUserId())) {
            throw new IllegalArgumentException("收藏夹不存在或无权限");
        }
    }

    @Override
    public boolean isFolderOwnedByUser(Long userId, Long folderId) {
        if (userId == null || folderId == null) return false;
        FavoriteFolder folder = favoriteFolderMapper.selectById(folderId);
        return folder != null && userId.equals(folder.getUserId());
    }
}
