package me.kzv.okvue.modules.account;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import me.kzv.okvue.infra.common.BaseEntity;

import javax.persistence.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
public class Account extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idx;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String nickname;

    private String password;

    private String profileImage;

    @Column(columnDefinition = "boolean default false")
    private boolean emailVerified;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Authority authority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoginType loginType;

    public Account updateProfile(String profileImage) {
        this.profileImage = profileImage;
        return this;
    }

    public String getAuthority(){
        return this.authority.getKey();
    }
}
