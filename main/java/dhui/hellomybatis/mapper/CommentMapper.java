package dhui.hellomybatis.mapper;

import dhui.hellomybatis.entity.Comment;
import org.apache.ibatis.annotations.*;

@Mapper
public interface CommentMapper {
    @Select("select * from comment where cid = #{cid}")
    public Comment findById(int cid);

    @Insert("insert into comment(content, author, aid) values (#{content}, #{author}, #{aid})")
    public int insert(Comment comment);

    @Update("update comment set content = #{content} where cid = #{cid}")
    public int update(Comment comment);

    @Delete("delete from comment where cid = #{cid}")
    public int delete(int cid);
}
