package cn.edu.neusoft.mall.entiy;

import java.util.Date;

public class FoundItem {

        private int id;
        private int lostItemId;
        private Date foundDate;
        private String status;
        private int user_id;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    // Getters and Setters
        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getLostItemId() {
            return lostItemId;
        }

        public void setLostItemId(int lostItemId) {
            this.lostItemId = lostItemId;
        }

        public Date getFoundDate() {
            return foundDate;
        }

        public void setFoundDate(Date foundDate) {
            this.foundDate = foundDate;
        }
    }

