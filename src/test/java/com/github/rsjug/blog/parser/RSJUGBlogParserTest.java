package com.github.rsjug.blog.parser;

import com.github.rsjug.blog.model.Blog;
import com.github.rsjug.blog.model.BlogPost;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by pestano on 02/11/16.
 */
public class RSJUGBlogParserTest {


    @Test
    public void shouldParseFeed() throws FileNotFoundException {

        File feed = new File(RSJUGBlogParser.class.getResource("/sample-feed.xml").getFile());
        assertThat(feed).isNotNull().exists();
        Blog blog = new RSJUGBlogParser().parse(new FileReader(feed));
        assertThat(blog).isNotNull();

        assertThat(blog.getTitle()).isEqualTo("Grupo de Usuários Java do Rio Grande do Sul");
        assertThat(blog.getPosts()).isNotEmpty().hasSize(1);

        BlogPost blogPost = blog.getPosts().get(0);
        assertThat(blogPost).extracting("title","url").
                contains("Histórico", "http://www.rsjug.org/?page_id=2");

        assertThat(blogPost.getContent()).contains("O RSJUG é o grupo de usuários da linguagem Java do Rio Grande do Sul - Brasil.");

    }
}
