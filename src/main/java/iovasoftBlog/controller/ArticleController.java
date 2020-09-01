package iovasoftBlog.controller;

import iovasoftBlog.bindingModel.ArticleBindingModel;
import iovasoftBlog.entity.Category;
import iovasoftBlog.entity.Tag;
import iovasoftBlog.repository.CategoryRepository;
import iovasoftBlog.repository.TagRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import iovasoftBlog.entity.Article;
import iovasoftBlog.entity.User;
import iovasoftBlog.repository.ArticleRepository;
import iovasoftBlog.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class ArticleController {
    @Autowired
    private ArticleRepository articleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TagRepository tagRepository;

    @GetMapping("/article/create")
    @PreAuthorize("isAuthenticated()")
    public String create (Model model) {
        model.addAttribute("view", "article/create");

        List<Category> categories = this.categoryRepository.findAll();
        model.addAttribute("categories", categories);


        return "base-layout";
    }

    @PostMapping("/article/create")
    @PreAuthorize("isAuthenticated()")
    public String createProcess(ArticleBindingModel articleBindingModel) {
        UserDetails user = (UserDetails) SecurityContextHolder
            .getContext()
            .getAuthentication()
            .getPrincipal();

        User userEntity = this.userRepository.findByEmail(user.getUsername());
        Category category = this.categoryRepository.getOne(articleBindingModel.getCategoryId());
        HashSet<Tag> tags = this.findTagsFromString(articleBindingModel.getTagString());

        Article articleEntity = new Article(
                articleBindingModel.getTitle(),
                articleBindingModel.getContent(),
                userEntity,
                category,
                tags
        );

        return "/redirect:/";
    }

    @GetMapping("/article/{id}")
    public String details(Model model, @PathVariable Integer id) {
        if (!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        if(!(SecurityContextHolder.getContext().getAuthentication() instanceof AnonymousAuthenticationToken)) {
            UserDetails principal = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            User entityUser = this.userRepository.findByEmail(principal.getUsername());
            model.addAttribute("user", entityUser);
        }

        Article article = this.articleRepository.getOne(id);
        model.addAttribute("article", article);
        model.addAttribute("view", "article/details");

        return "base-layout";
    }

    @GetMapping("/article/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String edit(@PathVariable Integer id, Model model) {
        if (!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        Article article = this.articleRepository.getOne(id);

        if(!isUserAuthorOrAdmin(article)) {
            return "redirect:/article/" + id;
        }

        List<Category> categories = this.categoryRepository.findAll();
        String tagString = article
                .getTags()
                .stream()
                .map(Tag::getName)
                .collect(Collectors.joining(", "));

        model.addAttribute("view", "article/edit");
        model.addAttribute("article", article);
        model.addAttribute("categories", categories);
        model.addAttribute("tags", tagString);

        return "base-layout";
    }

    @GetMapping("/article/edit/{id}")
    @PreAuthorize("isAuthenticated()")
    public String editProcess(@PathVariable Integer id, ArticleBindingModel articleBindingModel){
        if(!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        Article article = this.articleRepository.getOne(id);
        Category category = this.categoryRepository.getOne(articleBindingModel.getCategoryId());
        HashSet<Tag> tags = this.findTagsFromString(articleBindingModel.getTagString());

        if(!isUserAuthorOrAdmin(article)) {
            return "redirect:/article/" + id;
        }

        article.setContent(articleBindingModel.getContent());
        article.setTitle(articleBindingModel.getTitle());
        article.setCategory(category);
        article.setTags(tags);

        this.articleRepository.saveAndFlush(article);

        return "redirect:/article/" + article.getId();
    }

    @GetMapping("/article/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String delete (Model model, @PathVariable Integer id) {
        if(!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        Article article = this.articleRepository.getOne(id);

        if(!isUserAuthorOrAdmin(article)) {
            return "redirect:/article/" + id;
        }

        model.addAttribute("article", article);
        model.addAttribute("view", "article/delete");

        return "base-layout";
    }

    private boolean isUserAuthorOrAdmin(Article article) {
        UserDetails user = (UserDetails) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        User userEntity = this.userRepository.findByEmail(user.getUsername());

        return userEntity.isAdmin() || userEntity.isAuthor(article);
    }

    @GetMapping("/article/delete/{id}")
    @PreAuthorize("isAuthenticated()")
    public String deleteProcess (@PathVariable Integer id) {
        if(!this.articleRepository.existsById(id)) {
            return "redirect:/";
        }

        Article article = this.articleRepository.getOne(id);

        if(!isUserAuthorOrAdmin(article)) {
            return "redirect:/article/" + id;
        }

        this.articleRepository.delete(article);

        return "base-layout";
    }

    private HashSet<Tag> findTagsFromString(String tagString) {
        HashSet<Tag> tags = new HashSet<>();
        String[] tagNames = tagString.split(",\\s*");

        for(String tagName : tagNames) {
            Tag currentTag = this.tagRepository.findByName(tagName);

            if(currentTag == null) {
                currentTag = new Tag(tagName);
                this.tagRepository.saveAndFlush(currentTag);
            }

            tags.add(currentTag);
        }

        return tags;
    }
}
