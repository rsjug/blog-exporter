/**
 * Created by rafael-pestano on 01/11/16.
 */
package com.github.rsjug.blog.exporter;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.github.rsjug.blog.model.Blog;
import com.github.rsjug.blog.model.BlogPost;
import com.github.rsjug.blog.parser.RSJUGBlogParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
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

    @Parameter(names = "-p", description = "Path to wordpress feed.xml. Ex: -p /opt/feed.xml", required = true)
    private String path;

    @Parameter(names = "-outputDir", description = "Directory where posts will be exported. Default if 'exported'.", required = false)
    private String outputDir;

    @Parameter(names = "-layout", description = "Post layout name used in post front matter. Default if 'inner'.", required = false)
    private String layout;


    public static void main(String[] args) throws IOException {
        RSJUGBlogExporter.getInstance().execute(args);
    }

    public void execute(String args[]) throws IOException {
        JCommander commandLine = null;
        try {
            commandLine = new JCommander(this);
            commandLine.parse(args);
        } catch (ParameterException pe) {
            commandLine.usage();
            throw pe;
        }

        if(outputDir == null){
            outputDir = "exported/";
        }
        if(!outputDir.endsWith("/")){
            outputDir = outputDir+"/";
        }

        if(layout == null || "".equals(layout.trim())){
            layout = "inner";
        }

        FileReader reader = null;
        try {
            reader = new FileReader(new File(path));
            exportBlog(reader);
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
                forEach(this::exportPost);

        System.out.println(blog.getPosts().size() + " posts exported successfully!");
    }

    public void exportPost(BlogPost post) {
        //will not work inside fat jar, see http://stackoverflow.com/questions/20389255/reading-a-resource-file-from-within-jar
        //File template = new File(RSJUGBlogExporter.class.getResource("/post-template.md").getFile());
        InputStream inputStream = null;
        try {
            inputStream = RSJUGBlogExporter.class.getResourceAsStream("/post-template.md");
            StringWriter writer = new StringWriter();
            IOUtils.copy(inputStream, writer, "UTF-8");
            String postContent = writer.toString();
            String originalContent = post.getContent();
            originalContent = extractImages(originalContent);
            String postDateTime = POST_DATE_TIME_FORMAT.format(post.getPublishedOn());
            String postDate = POST_DATE_FORMAT.format(post.getPublishedOn());
            postContent = postContent.replace("{title}", post.getTitle()).
                    replace("{date}", postDateTime).
                    replace("{layout}", layout).
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

            File exportedPost = new File(outputDir + postFileName);
            FileUtils.writeStringToFile(exportedPost, postContent, Charset.forName("UTF-8"));


        } catch (Exception e) {
            Logger.getLogger(RSJUGBlogExporter.class.getName()).log(Level.WARNING, "Problem to parse post " + post.getTitle(), e);
        } finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
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
            File outputImage = new File(outputDir+"img/" + imageName);
            outputImage.mkdirs();
            ImageIO.write(image, imageFormat, outputImage);
        } catch (Exception e) {
            Logger.getLogger(RSJUGBlogExporter.class.getName()).log(Level.WARNING, "Could not save image " + imageUrl, e);
        }
    }
}
