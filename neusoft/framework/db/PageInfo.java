package cn.edu.neusoft.framework.db;

import java.util.List;

public class PageInfo<T> {
	
	private List<T> list;
	private long totalPage;
	private long recNum;
	public List<T> getList() {
		return list;
	}
	public void setList(List<T> list) {
		this.list = list;
	}
	public long getTotalPage() {
		return totalPage;
	}
	public void setTotalPage(long totalPage) {
		this.totalPage = totalPage;
	}
	public long getRecNum() {
		return recNum;
	}
	public void setRecNum(long recNum) {
		this.recNum = recNum;
	}
	@Override
	public String toString() {
		return "PageInfo [list=" + list + ", totalPage=" + totalPage + ", recNum=" + recNum + "]";
	}
	

}
