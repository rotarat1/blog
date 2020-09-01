package iovasoftBlog.repository;
import iovasoftBlog.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import iovasoftBlog.entity.Role;

public interface RoleRepository extends JpaRepository<Role, Integer>{
    Role findByName (String name);
}
