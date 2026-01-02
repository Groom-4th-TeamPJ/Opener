package spring.backend.domain.user.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.backend.domain.user.model.enums.Role;
import spring.backend.shared.entity.BaseEntity;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column
  private String name;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private Role role = Role.USER;

  public static User createUser(String name) {
    return createUser(name, Role.USER);
  }

  public static User createUser(String name, Role role) {
    User user = new User();
    user.name = name;
    user.role = role;
    return user;
  }
}
