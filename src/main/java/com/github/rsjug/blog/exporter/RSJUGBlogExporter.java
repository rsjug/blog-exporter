/**
 * Created by rafael-pestano on 01/11/16.
 */
package com.github.rsjug.blog.exporter;

import com.github.rsjug.blog.parser.RSJUGBlogParser;
import com.sangupta.blogparser.domain.Blog;
import com.sangupta.blogparser.domain.BlogPost;
import com.sangupta.blogparser.wordpress.WordpressParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

public class RSJUGBlogExporter {
    private static SimpleDateFormat POST_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat POST_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static void main(String[] args) throws FileNotFoundException {
        FileReader reader = new FileReader(RSJUGBlogExporter.class.getResource("/rsjug.xml").getFile());
        WordpressParser parser = new RSJUGBlogParser();
        Blog blog = parser.parse(reader);

        blog.getPosts().stream().
                filter(post -> post.getContent() != null
                                && !"".equals(post.getContent().trim())
                ).
                sorted(comparing(BlogPost::getPublishedOn).reversed()).
                forEach(post -> export(post));

    }

    private static void export(BlogPost post) {
        File template = new File(RSJUGBlogExporter.class.getResource("/post-template.md").getFile());
        try {
            String postContent = FileUtils.readFileToString(template, Charset.forName("UTF-8"));
            String postDateTime = POST_DATE_TIME_FORMAT.format(post.getPublishedOn());
            String postDate = POST_DATE_FORMAT.format(post.getPublishedOn());
            postContent = postContent.replace("{title}", post.getTitle()).
                    replace("{date}", postDateTime).
                    replace("{categories}", post.getCategories() != null ? post.getCategories().stream().
                            collect(Collectors.joining(" ")) : "").
                    replace("{tags}", post.getTags() != null ? post.getTags().stream().
                            collect(Collectors.joining(" ")) : "").
                    replace("{load-text}", post.getContent().substring(0, 255).concat("...")).
                    replace("{content}", post.getContent());
            StringBuilder postName = new StringBuilder(postDate).append("-").
                    append(post.getTitle()).append(".md");

            File exportedPost = new File("exported/"+postName.toString().replace("/", " ").replaceAll(" ", "-"));
            FileUtils.writeStringToFile(exportedPost, postContent, Charset.forName("UTF-8"));


        } catch (Exception e) {
            Logger.getLogger(RSJUGBlogExporter.class.getName()).log(Level.WARNING, "Problem to parse post " + post.getTitle(), e);
        }

    }
}
