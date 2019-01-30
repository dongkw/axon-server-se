package io.axoniq.cli.json;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Marc Gathier
 */
public class Application {

    private String name;

    private String description;

    private String token;

    private Set<ApplicationContext> roles = new HashSet<>();

    public Application() {
    }

    public Application(String name, String description, String token, String[] roles) {
        this.name = name;
        this.description = description;
        this.token = token;
        if( roles != null) {
            Map<String, List<String>> rolesPerContext = new HashMap<>();
            Arrays.stream(roles).forEach(r -> {
                if( r.contains("@")) {
                    String[] roleAndContext = r.split("@", 2);
                    rolesPerContext.computeIfAbsent(roleAndContext[1], c -> new ArrayList<>()).add(roleAndContext[0]);
                } else {
                    rolesPerContext.computeIfAbsent("default", c -> new ArrayList<>()).add(r);
                }
            });

            rolesPerContext.forEach((context,roleList) -> this.roles.add(new ApplicationContext(context, roleList)));
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<ApplicationContext> getRoles() {
        return roles;
    }

    public void setRoles(Set<ApplicationContext> roles) {
        this.roles = roles;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
