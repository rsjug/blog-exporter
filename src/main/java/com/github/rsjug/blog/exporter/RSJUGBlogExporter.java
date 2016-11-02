/**
 * Created by rafael-pestano on 01/11/16.
 */
package com.github.rsjug.blog.exporter;

import com.github.rsjug.blog.model.Blog;
import com.github.rsjug.blog.model.BlogPost;
import com.github.rsjug.blog.parser.RSJUGBlogParser;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;

public class RSJUGBlogExporter {
    private static SimpleDateFormat POST_DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static SimpleDateFormat POST_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static RSJUGBlogExporter instance;


    public static void main(String[] args) throws IOException {
        FileReader reader = null;
        try {
            reader = new FileReader(RSJUGBlogExporter.class.getResource("/rsjug.xml").getFile());
            RSJUGBlogExporter.getInstance().exportBlog(reader);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

    }

    public static RSJUGBlogExporter getInstance() {
        if (instance == null) {
            instance = new RSJUGBlogExporter();
        }
        return instance;
    }

    public void exportBlog(FileReader blogFeed) {
        RSJUGBlogParser parser = new RSJUGBlogParser();
        Blog blog = parser.parse(blogFeed);

        blog.getPosts().stream().
                filter(post -> post.getContent() != null
                                && !"".equals(post.getContent().trim())
                ).
                sorted(comparing(BlogPost::getPublishedOn).reversed()).
                forEach(post -> exportPost(post));

        System.out.println(blog.getPosts().size() + " posts exported successfully!");
    }

    public void exportPost(BlogPost post) {
        File template = new File(RSJUGBlogExporter.class.getResource("/post-template.md").getFile());
        try {
            String postContent = FileUtils.readFileToString(template, Charset.forName("UTF-8"));
            String originalContent = post.getContent();
            originalContent = extractImages(originalContent);
            String postDateTime = POST_DATE_TIME_FORMAT.format(post.getPublishedOn());
            String postDate = POST_DATE_FORMAT.format(post.getPublishedOn());
            postContent = postContent.replace("{title}", post.getTitle()).
                    replace("{date}", postDateTime).
                    replace("{author}", post.getAuthor() != null ? post.getAuthor().getName() : "").
                    replace("{categories}", post.getCategories() != null ? post.getCategories().stream().
                            collect(Collectors.joining(" ")) : "").
                    replace("{tags}", post.getTags() != null ? post.getTags().stream().
                            collect(Collectors.joining(" ")) : "").
                    replace("{load-text}", getLoadText(originalContent)).
                    replace("{content}", originalContent);
            String postFileName = new StringBuilder(postDate).append("-").
                    append(post.getTitle().replace("\"", "").replace("/", " ").replaceAll(" ", "-")).
                    append(".md").toString();

            File exportedPost = new File("exported/" + postFileName);
            FileUtils.writeStringToFile(exportedPost, postContent, Charset.forName("UTF-8"));


        } catch (Exception e) {
            Logger.getLogger(RSJUGBlogExporter.class.getName()).log(Level.WARNING, "Problem to parse post " + post.getTitle(), e);
        }

    }

    private String getLoadText(String originalContent) {
        String textWithoutHtmlTags = Jsoup.parse(originalContent.trim()).text();
        String loadText = "";
        if (textWithoutHtmlTags != null && !textWithoutHtmlTags.isEmpty()) {
            loadText = textWithoutHtmlTags;
        } else {
            loadText = originalContent.trim();
        }
        return loadText.length() > 255 ? loadText.substring(0, 255).concat("...") : loadText;

    }

    private String extractImages(String content) {
        Document html = Jsoup.parse(content);
        Elements postImages = html.select("img");
        for (Element postImage : postImages) {
            String imageUrl = postImage.attr("src");
            //changes absolute url to img/imageName because images will be in img folder in local server
            content = content.replace(imageUrl, "img/" + imageUrl.substring(imageUrl.lastIndexOf("/") + 1));
            saveImages(imageUrl);
        }
        return content;
    }

    private void saveImages(String imageUrl) {
        BufferedImage image;
        try {
            String imageName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
            String imageFormat = imageName.substring(imageName.lastIndexOf(".") + 1);

            URL url = new URL(imageUrl);
            image = ImageIO.read(url);
            File outputImage = new File("exported/img/" + imageName);
            outputImage.mkdirs();
            ImageIO.write(image, imageFormat, outputImage);
        } catch (Exception e) {
            Logger.getLogger(RSJUGBlogExporter.class.getName()).log(Level.WARNING, "Could not save image " + imageUrl, e);
        }
    }
}
