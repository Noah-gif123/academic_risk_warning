    package cn.edu.neusoft.mall.service;

    import cn.edu.neusoft.mall.entiy.LostTing;
    import java.util.List;
    import java.util.Map;

    public interface LostTingService {

        public List<LostTing> getlostTingList();
        public List<LostTing> getLostTingListByName(String keyword);
        LostTing getById(int id);
        public int insertLostTing(LostTing lostTing);
        Map<String, Object> publishItem(LostTing lostTing, Integer userId);
        double getRecoveryRate();
        List<LostTing> getLostTingListByUserId(int userId);
        boolean updateStatus(int itemId, String status);

    }