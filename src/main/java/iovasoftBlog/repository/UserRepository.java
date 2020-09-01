package iovasoftBlog.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import iovasoftBlog.entity.User;

public interface UserRepository extends JpaRepository<User, Integer>{
    User findByEmail (String email);
}
