public class Admin {
    private String id;
    private String name;
    private String email;
    private String adminId;

    public Admin(String id, String name, String email, String adminId) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.adminId = adminId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getAdminId() { return adminId; }

    // UPDATED: Now just opens the link instead of trying to insert an enrollment record
    public void joinClass() {
        JoinClasses.openClassLinkAsAdmin();
    }
}