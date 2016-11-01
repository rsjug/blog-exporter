package com.github.rsjug.blog.parser;

import com.sangupta.blogparser.domain.Author;
import com.sangupta.blogparser.domain.Blog;
import com.sangupta.blogparser.domain.BlogPost;
import com.sangupta.blogparser.wordpress.WordpressParser;
import com.sun.syndication.feed.synd.SyndCategoryImpl;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.FeedException;
import com.sun.syndication.io.SyndFeedInput;
import org.jdom.Element;

import java.io.Reader;
import java.util.List;
import java.util.Optional;

/**
 * Created by rafael-pestano on 01/11/16.
 */
public class RSJUGBlogParser extends WordpressParser {


    private static final String PUBLISHED = "publish";

    public Blog parse(Reader reader) {
        if(reader == null) {
            throw new IllegalArgumentException("Reader cannot be null.");
        }

        SyndFeed feed = null;
        try {
            feed = new SyndFeedInput().build(reader);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Illegal arguments when parsing feed", e);
        } catch (FeedException e) {
            throw new RuntimeException("Unable to parse feed", e);
        }

        Blog blog = new Blog();
        blog.setTitle(feed.getTitle());
        blog.setUrl(feed.getLink());
        blog.setDescription(feed.getDescription());

        // do for each entry
        for(Object object : feed.getEntries()) {
            SyndEntryImpl entry = (SyndEntryImpl) object;

            // get the title
            String postStatus = getStatus(entry);
            if(!PUBLISHED.equals(postStatus)){
                continue;
            }
            BlogPost post = new BlogPost();
            post.setTitle(entry.getTitle());
            post.setContent(((SyndContentImpl) entry.getContents().get(0)).getValue());
            post.setUrl(entry.getLink());
            post.setPublishedOn(entry.getPublishedDate());

            if(entry.getAuthors() == null || entry.getAuthors().isEmpty()) {
                Author author = new Author();
                author.setName(entry.getAuthor());
                post.setAuthor(author);
            } else {
                // TODO: fix issue with multiple authors
            }

            // extract the categories
            if(entry.getCategories() != null && !entry.getCategories().isEmpty()) {
                for(Object cat : entry.getCategories()) {
                    SyndCategoryImpl category = (SyndCategoryImpl) cat;

                    String taxonomyUri = category.getTaxonomyUri();
                    if(taxonomyUri != null) {
                        if("category".equals(taxonomyUri)) {
                            post.addCategory(category.getName());
                        } else if("tag".equals(taxonomyUri)) {
                            post.addTag(category.getName());
                        }
                    }
                }
            }

            // add to the blog object
            blog.addPost(post);
        }

        return blog;
    }

    private String getStatus(SyndEntryImpl entry) {
        List<Element> wordpressElements = (List<Element>) entry.getForeignMarkup();

        Optional<Element> status = wordpressElements.stream().
                filter(e -> "status".equals(e.getName())).
                findFirst();

        if(status.isPresent()){
            return status.get().getValue();
        } else {
            return null;
        }

    }
}
