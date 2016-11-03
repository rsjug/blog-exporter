package com.github.rsjug.blog.exporter;

import com.github.rsjug.blog.parser.RSJUGBlogParser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.contentOf;

/**
 * Created by pestano on 02/11/16.
 */
public class RSJUGBlogExporterTest {

    private static final String PATH = Paths.get("target/exported-test/").toAbsolutePath().toString();
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static RSJUGBlogExporter blogExporter;

    @BeforeClass
    public static void setup() {
        blogExporter = RSJUGBlogExporter.getInstance();
    }

    @Test
    public void shouldExportBlog() throws FileNotFoundException {
        blogExporter.outputDir = PATH;
        blogExporter.exportBlog(new FileReader
                (new File(RSJUGBlogParser.class.getResource("/sample-feed.xml").getFile())));
        File generatedPost = new File(PATH +"/2008-10-11-Histórico.md");
        assertThat(generatedPost).exists();
        assertThat(contentOf(generatedPost)).startsWith("---"+NEW_LINE +
                "layout: inner"+NEW_LINE +
                "title: 'Histórico'"+NEW_LINE +
                "date: 2008-10-11 11:08:47"+NEW_LINE +
                "categories: Uncategorized"+NEW_LINE +
                "tags: "+NEW_LINE +
                "post_author: admin"+NEW_LINE +
                "comments: false"+NEW_LINE +
                "lead_text: 'O RSJUG é o grupo de usuários da linguagem Java do Rio Grande do Sul - Brasil. O JUG começou na UFRGS (Universidade Federal do Rio Grande do Sul) , entre os alunos do Centro de Pós-Graduação em Ciências da Computaçao. O principal idealizador e primeiro JU...'"+NEW_LINE +
                "---");
        assertThat(contentOf(generatedPost)).contains("<p style=\"text-align: justify;\">O RSJUG é o grupo de usuários da linguagem Java do Rio Grande do Sul - Brasil.");

    }


}
