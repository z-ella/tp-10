package ma.fstg.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Automatise les scenarios des etapes 7 et 8 du TP. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFlowTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void accesSansLogin_redirigeVersLogin() throws Exception {
        mvc.perform(get("/user/dashboard"))
           .andExpect(status().is3xxRedirection())
           .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void pageLogin_estAccessible() throws Exception {
        mvc.perform(get("/login"))
           .andExpect(status().isOk())
           .andExpect(view().name("login"));
    }

    @Test
    void connexionReussie_redirigeVersHome() throws Exception {
        mvc.perform(formLogin("/authenticate").user("user").password("1111"))
           .andExpect(authenticated().withUsername("user"))
           .andExpect(redirectedUrl("/home"));
    }

    @Test
    void mauvaisMotDePasse_retourneSurLoginError() throws Exception {
        mvc.perform(formLogin("/authenticate").user("user").password("mauvais"))
           .andExpect(unauthenticated())
           .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void deconnexion_redirigeVersLoginLogout() throws Exception {
        mvc.perform(post("/logout").with(csrf()))
           .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userSansRoleAdmin_obtient403() throws Exception {
        mvc.perform(get("/admin/dashboard"))
           .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void userAccedeASonEspace() throws Exception {
        mvc.perform(get("/user/dashboard"))
           .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminAccedeAuxDeuxEspaces() throws Exception {
        mvc.perform(get("/admin/dashboard")).andExpect(status().isOk());
        mvc.perform(get("/user/dashboard")).andExpect(status().isOk());
    }
}
