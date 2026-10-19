package com.gym.member.service;

import java.util.Optional;

import com.gym.core.dao.MemberDao;
import com.gym.core.dao.PageResult;
import com.gym.core.model.Member;

/**
 * Tầng Service cho Module Hội viên (TV2).
 * Đứng giữa UI và DAO để điều phối luồng dữ liệu và nghiệp vụ.
 */
public class MemberService {

    private final MemberDao memberDao;

    public MemberService() {
        this.memberDao = new MemberDao();
    }

    /**
     * Tìm kiếm hội viên theo từ khóa (tên, SĐT, email) có phân trang.
     */
    public PageResult<Member> search(String keyword, int page, int pageSize) {
        return memberDao.search(keyword, page, pageSize);
    }

    /**
     * Lấy ảnh chân dung của hội viên.
     */
    public Optional<byte[]> getPhoto(int visitorId) {
        return memberDao.findPhoto(visitorId);
    }

    /**
     * Lưu ảnh chân dung hội viên.
     */
    public void savePhoto(int visitorId, byte[] photo) {
        memberDao.savePhoto(visitorId, photo);
    }
}