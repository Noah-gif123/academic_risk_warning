package cn.edu.neusoft.framework.db;

import org.apache.commons.dbcp2.BasicDataSource;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;


public class JDBCTemplate {
	private static DataSource dataSource=null;
	// 查询结果分页时，每页显示记录数。
	private static final long PAGE_REC_NUM = 6;
	static{
		ResourceBundle bundle=ResourceBundle.getBundle("jdbc");
		BasicDataSource ds=new BasicDataSource();
		ds.setDriverClassName(bundle.getString("driverClassName"));
		ds.setUrl(bundle.getString("url"));
		ds.setUsername(bundle.getString("username"));
		ds.setPassword(bundle.getString("password"));
		ds.setInitialSize(Integer.parseInt(bundle.getString("initialSize")));
		ds.setMaxTotal(Integer.parseInt(bundle.getString("maxTotal")));
		ds.setMaxIdle(Integer.parseInt(bundle.getString("maxIdle")));
		dataSource=ds;
	}
	private JDBCTemplate() {

	}
	private static Connection getConnection() throws SQLException{
		Connection conn=dataSource.getConnection();
		return conn;
		
	}
	public static <T> T queryForObject(String sql,RowMapper<T> rowMapper,Object... args) throws Exception{
		Connection conn=null;
		T result=null;
	 try{
		 conn=getConnection();
		 PreparedStatement pstmt=conn.prepareStatement(sql);
		 if(args!=null){
			 for(int i=0;i<args.length;i++){
				 pstmt.setObject(i+1, args[i]); 
			 }
		 }
		 ResultSet rs=pstmt.executeQuery();
		 ResultSetMetaData rsmd=rs.getMetaData();
		 int cols=rsmd.getColumnCount();
		 String[] colNames=new String[cols];
		 for(int i=0;i<cols;i++){
			 colNames[i]=rsmd.getColumnLabel(i+1);
		 }
		 if(rs.next()){
				result= rowMapper.mapRow(rs, 1);
		 }else{
			return result;
		 }
		
		 rs.close();
		 pstmt.close();
		 
	 }catch(SQLException e){
		 e.printStackTrace();
	 }finally{
		try{
		 if(conn!=null){
			 conn.close();
		 }
		 }catch(SQLException e){
			 e.printStackTrace();
		 }
	 }
	 return result;
	}
	public static  List queryForList(String sql,Object ... args){
		Connection conn=null;
		List list=new ArrayList();
	 try{
		 conn=getConnection();
		 PreparedStatement pstmt=conn.prepareStatement(sql);
		 if(args!=null){
			 for(int i=0;i<args.length;i++){
				 pstmt.setObject(i+1, args[i]); 
			 }
		 }
		 ResultSet rs=pstmt.executeQuery();
		 ResultSetMetaData rsmd=rs.getMetaData();
		 int cols=rsmd.getColumnCount();
		 String[] colNames=new String[cols];
		 for(int i=0;i<cols;i++){
			 colNames[i]=rsmd.getColumnLabel(i+1);
		 }
		 while(rs.next()){
			 Map row=new HashMap();
			 for(int i=0;i<cols;i++){
				 row.put(colNames[i], rs.getObject(i+1));
			 }
			 list.add(row);
		 }
		 rs.close();
		 pstmt.close();
		 
	 }catch(SQLException e){
		 e.printStackTrace();
	 }finally{
		try{
		 if(conn!=null){
			 conn.close();
		 }
		 }catch(SQLException e){
			 e.printStackTrace();
		 }
	 }
	 return list;
	}
	public static <T>   List<T>  query(String sql,RowMapper<T> rowMapper,Object ... args){
		Connection conn=null;
		List<T> list=new ArrayList<T>();
	 try{
		 conn=getConnection();
		 PreparedStatement pstmt=conn.prepareStatement(sql);
		 if(args!=null){
			 for(int i=0;i<args.length;i++){
				 pstmt.setObject(i+1, args[i]); 
			 }
		 }
		 ResultSet rs=pstmt.executeQuery();
		 ResultSetMetaData rsmd=rs.getMetaData();
		 int cols=rsmd.getColumnCount();
		 String[] colNames=new String[cols];
		 for(int i=0;i<cols;i++){
			 colNames[i]=rsmd.getColumnLabel(i+1);
		 }
		 int count=0;
		 while(rs.next()){
			 count++;
			 T t=rowMapper.mapRow(rs, count);
			 list.add(t);
		 }
		 rs.close();
		 pstmt.close();
		 
	 }catch(SQLException e){
		 e.printStackTrace();
	 }finally{
		try{
		 if(conn!=null){
			 conn.close();
		 }
		 }catch(SQLException e){
			 e.printStackTrace();
		 }
	 }
	 return list;
	}
	public static Object[][] queryForArray(String sql,Object ... args){
		Connection conn=null;
		Object[][] result=null;
	 try{
		 conn=getConnection();
		 PreparedStatement pstmt=conn.prepareStatement(sql,ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);
		 if(args!=null){
			 for(int i=0;i<args.length;i++){
				 pstmt.setObject(i+1, args[i]); 
			 }
		 }
		 ResultSet rs=pstmt.executeQuery();
		 rs.last();
		 int total=rs.getRow();
		 result=new Object[total][];
		 rs.beforeFirst();
		 ResultSetMetaData rsmd=rs.getMetaData();
		 int cols=rsmd.getColumnCount();
		 int rownum=0;
		 while(rs.next()){
			 result[rownum]=new Object[cols];
			 
			 for(int i=0;i<cols;i++){
				 result[rownum][i]=rs.getObject(i+1);
			 }
			 rownum++;
		 }
		 rs.close();
		 pstmt.close();
		 
	 }catch(SQLException e){
		 e.printStackTrace();
	 }finally{
		try{
		 if(conn!=null){
			 conn.close();
		 }
		 }catch(SQLException e){
			 e.printStackTrace();
		 }
	 }
	 return result;
	}
	public static int update(String sql,Object ... args){
		Connection conn=null;
		int count=-1;
		try{
			conn=getConnection();
			 PreparedStatement pstmt=conn.prepareStatement(sql);
			 if(args!=null){
				 for(int i=0;i<args.length;i++){
					 pstmt.setObject(i+1, args[i]); 
				 }
			 }
			 count=pstmt.executeUpdate();
			pstmt.close();
			
		}catch(SQLException ex){
			ex.printStackTrace();
		}finally{
			if(conn!=null){
				try {
					conn.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return count;
	}
	public static  Map queryForMap(String sql,Object ... params) throws Exception{
		  List result=queryForList(sql,params);
		  if(result.size()==1){
			return (Map)result.get(0);  
		  }else{
			  throw new Exception("期望一行数据，实际返回"+result.size()+"行");
		  }
		  
		}
	
	// 查询结果分页时，返回分页信息的Map，包括总页数，每页记录数和当前页中的记录。
	public static Map<String, Object> getPage(String sql, Object[] params, int curPage) throws Exception {
			Map<String, Object> page = new HashMap<String, Object>();
			String newSql = sql + " limit " + (curPage - 1) * PAGE_REC_NUM + "," + PAGE_REC_NUM;
			System.out.println(newSql);
			List<Map<String, String>> pageList = queryForList(newSql, params);
			sql = sql.toLowerCase();
			String countSql = "";
			if (sql.indexOf("group") >= 0) {
				countSql = "select count(*) as tempNum from (" + sql + ") as temp";
			} else {
				countSql = "select count(*) as tempNum " + sql.substring(sql.indexOf("from"));
			}
			Long count = (Long) queryForMap(countSql, params).get("tempNum");
			long totalPage = 0;
			if (count % PAGE_REC_NUM == 0)
				totalPage = count / PAGE_REC_NUM;
			else
				totalPage = count / PAGE_REC_NUM + 1;
			page.put("list", pageList);
			page.put("totalPage", totalPage);
			page.put("recNum", PAGE_REC_NUM);
			return page;
		}
	// 查询结果分页时，返回分页信息的PageInfo，包括总页数，每页记录数和当前页中的记录。
		public static <T> PageInfo<T> getPage(String sql,RowMapper<T> rowMapper, Object[] params, int curPage) throws Exception {
			PageInfo<T> page = new PageInfo<T>();
				String newSql = sql + " limit " + (curPage - 1) * PAGE_REC_NUM + "," + PAGE_REC_NUM;
				System.out.println(newSql);
				List<T> pageList = query(newSql,rowMapper, params);
				sql = sql.toLowerCase();
				String countSql = "";
				if (sql.indexOf("group") >= 0) {
					countSql = "select count(*) as tempNum from (" + sql + ") as temp";
				} else {
					countSql = "select count(*) as tempNum " + sql.substring(sql.indexOf("from"));
				}
				Long count = (Long) queryForMap(countSql, params).get("tempNum");
				long totalPage = 0;
				if (count % PAGE_REC_NUM == 0)
					totalPage = count / PAGE_REC_NUM;
				else
					totalPage = count / PAGE_REC_NUM + 1;
				page.setList(pageList);
				page.setTotalPage(totalPage);
				page.setRecNum(PAGE_REC_NUM);
				return page;
			}

		public static Map<String, Object> getPage(String sql, int curPage) throws Exception {
			return getPage(sql, new Object[] {}, curPage);
		}



}
