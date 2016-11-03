package com.github.rsjug.blog.exporter;

import com.github.rsjug.blog.parser.RSJUGBlogParser;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;
import static org.junit.Assert.assertTrue;

/**
 * Created by pestano on 02/11/16.
 */
public class RSJUGBlogExporterTest {

    private static final String BASE_PATH = Paths.get("target/exported-test/").toAbsolutePath().toString();
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static RSJUGBlogExporter blogExporter;

    PrintStream newOut;
    PrintStream defaultOut;
    ByteArrayOutputStream baos;

    @BeforeClass
    public static void setup() {
        blogExporter = RSJUGBlogExporter.getInstance();
    }

    @Before
    public void init() {
        baos = new ByteArrayOutputStream();
        newOut = new PrintStream(baos);
        defaultOut = System.out;
        System.setOut(newOut);
        System.out.flush();
        File exportDir = new File(BASE_PATH);
        if(exportDir.exists()){
            exportDir.delete();
        }
    }

    @After
    public void tearDown() throws IOException {
        System.setOut(defaultOut);
        baos.close();
    }

    @Test
    public void shouldExportBlog() throws FileNotFoundException {
        blogExporter.outputDir = BASE_PATH;
        blogExporter.exportBlog(new FileReader
                (new File(RSJUGBlogParser.class.getResource("/sample-feed.xml").getFile())));
        File generatedPost = new File(BASE_PATH +"/2008-10-11-Histórico.md");
        assertThat(generatedPost).exists();
        assertThat(contentOf(generatedPost)).contains(
                "categories: Uncategorized" + NEW_LINE +
                "tags: " + NEW_LINE +
                "post_author: admin" + NEW_LINE +
                "comments: false" + NEW_LINE +
                "lead_text: 'O RSJUG é o grupo de usuários da linguagem Java do Rio Grande do Sul - Brasil. O JUG começou na UFRGS (Universidade Federal do Rio Grande do Sul) , entre os alunos do Centro de Pós-Graduação em Ciências da Computaçao. O principal idealizador e primeiro JU...'" + NEW_LINE +
                "---");
        assertThat(contentOf(generatedPost)).contains("<p style=\"text-align: justify;\">O RSJUG é o grupo de usuários da linguagem Java do Rio Grande do Sul - Brasil.");

    }

    @Test
    public void shouldExportBlogViaCommandLine() throws IOException {
        new RSJUGBlogExporter().execute(new String[]{
                "-outputDir", "\""  + BASE_PATH +"/command-line/" +"\"",
                "-p", "\""  + RSJUGBlogParser.class.getResource("/sample-feed.xml").getFile() +"\""
        });
        File generatedPost = new File(BASE_PATH +"/command-line/2008-10-11-Histórico.md");
        assertThat(generatedPost).exists();
        File images = new File(BASE_PATH +"/img");
        assertThat(images).exists().isDirectory();

    }

    @Test
    public void shouldNotExportBlogWithoutPosts() throws IOException {
        new RSJUGBlogExporter().execute(new String[]{
                "-outputDir", "\""  + BASE_PATH +"/no-posts/" +"\"",
                "-p", "\""  + RSJUGBlogParser.class.getResource("/sample-feed-without-published-posts.xml").getFile() +"\""
        });
        File exportDir = new File(BASE_PATH +"/no-posts/");
        assertThat(exportDir).doesNotExist();
        File imagesDir = new File(BASE_PATH +"/no-posts/img");
        assertThat(imagesDir).doesNotExist();
        String output = baos.toString();
        assertThat(output).startsWith("No posts found.");
    }


}
