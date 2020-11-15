package rest.addressbook;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;


import java.io.IOException;
import java.net.URI;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.junit.After;
import org.junit.Test;
import rest.addressbook.config.ApplicationConfig;
import rest.addressbook.domain.AddressBook;
import rest.addressbook.domain.Person;

/**
 * A simple test suite.
 * <ul>
 *   <li>Safe and idempotent: verify that two identical consecutive requests do not modify
 *   the state of the server.</li>
 *   <li>Not safe and idempotent: verify that only the first of two identical consecutive
 *   requests modifies the state of the server.</li>
 *   <li>Not safe nor idempotent: verify that two identical consecutive requests modify twice
 *   the state of the server.</li>
 * </ul>
 */
public class AddressBookServiceTest {

  private HttpServer server;

  @Test
  public void serviceIsAlive() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    launchServer(ab);

    // Request the address book
    Client client = ClientBuilder.newClient();
    Response response = client.target("http://localhost:8282/contacts")
      .request().get();
    assertEquals(200, response.getStatus());
    assertEquals(0, response.readEntity(AddressBook.class).getPersonList()
      .size());
    //////////////////////////////////////////////////////////////////////
    // Verify that GET /contacts is well implemented by the service, i.e
    // complete the test to ensure that it is safe and idempotent
    //////////////////////////////////////////////////////////////////////

    // Verify that the method is safe
    // ab object didn't changed
    assertEquals(0, ab.getPersonList().size());

    //Verify if the method is idempotent
    Response new_response = client.target("http://localhost:8282/contacts")
            .request().get();
    assertEquals(200, new_response.getStatus());
    assertEquals(0, new_response.readEntity(AddressBook.class).getPersonList()
            .size());

  }

  @Test
  public void createUser() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    launchServer(ab);

    // Prepare data
    Person juan = new Person();
    juan.setName("Juan");
    URI juanURI = URI.create("http://localhost:8282/contacts/person/1");

    // Create a new user
    Client client = ClientBuilder.newClient();
    Response response = client.target("http://localhost:8282/contacts")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(juan, MediaType.APPLICATION_JSON));

    assertEquals(201, response.getStatus());
    assertEquals(juanURI, response.getLocation());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person juanUpdated = response.readEntity(Person.class);
    assertEquals(juan.getName(), juanUpdated.getName());
    assertEquals(1, juanUpdated.getId());
    assertEquals(juanURI, juanUpdated.getHref());

    // Verify if the method is not safe
    // ab object must has one person on the person list
    assertEquals(1, ab.getPersonList().size());

    // Check that the new user exists
    response = client.target("http://localhost:8282/contacts/person/1")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    juanUpdated = response.readEntity(Person.class);
    assertEquals(juan.getName(), juanUpdated.getName());
    assertEquals(1, juanUpdated.getId());
    assertEquals(juanURI, juanUpdated.getHref());

    //////////////////////////////////////////////////////////////////////
    // Verify that POST /contacts is well implemented by the service, i.e
    // complete the test to ensure that it is not safe and not idempotent
    //////////////////////////////////////////////////////////////////////

    // Verify that the method is not idempotent -> new petition doesn't return the same
    Response new_response = client.target("http://localhost:8282/contacts")
            .request(MediaType.APPLICATION_JSON)
            .post(Entity.entity(juan, MediaType.APPLICATION_JSON));
    assertEquals(201, new_response.getStatus());
    assertNotEquals(new_response.getLocation(), juanURI); // The uris must be different
    Person new_juanUpdated = new_response.readEntity(Person.class);
    assertNotEquals(new_juanUpdated.getId(), juanUpdated.getId());

    //Optional -> Check if the new user juan exists (same format as lines 99-106)

    // Verify if the method is not safe
    // ab object must has one person on the person list
    assertEquals(2, ab.getPersonList().size());
  }

  @Test
  public void createUsers() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    salvador.setId(ab.nextId());
    ab.getPersonList().add(salvador);
    launchServer(ab);

    // Prepare data
    Person juan = new Person();
    juan.setName("Juan");
    URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
    Person maria = new Person();
    maria.setName("Maria");
    URI mariaURI = URI.create("http://localhost:8282/contacts/person/3");

    // Create a user
    Client client = ClientBuilder.newClient();
    Response response = client.target("http://localhost:8282/contacts")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(juan, MediaType.APPLICATION_JSON));
    assertEquals(201, response.getStatus());
    assertEquals(juanURI, response.getLocation());

    // Create a second user
    response = client.target("http://localhost:8282/contacts")
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(maria, MediaType.APPLICATION_JSON));
    assertEquals(201, response.getStatus());
    assertEquals(mariaURI, response.getLocation());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person mariaUpdated = response.readEntity(Person.class);
    assertEquals(maria.getName(), mariaUpdated.getName());
    assertEquals(3, mariaUpdated.getId());
    assertEquals(mariaURI, mariaUpdated.getHref());

    // Check that the new user exists
    response = client.target("http://localhost:8282/contacts/person/3")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    mariaUpdated = response.readEntity(Person.class);
    assertEquals(maria.getName(), mariaUpdated.getName());
    assertEquals(3, mariaUpdated.getId());
    assertEquals(mariaURI, mariaUpdated.getHref());

    //////////////////////////////////////////////////////////////////////
    // Verify that GET /contacts/person/3 is well implemented by the service, i.e
    // complete the test to ensure that it is safe and idempotent
    //////////////////////////////////////////////////////////////////////

    // Verify if the method is safe
    // ab object must has three persons on the person list (Salvador,juan,maria)
    assertEquals(3, ab.getPersonList().size());

    //Verify if the method is idempotent
    Response new_response = client.target("http://localhost:8282/contacts/person/3")
            .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, new_response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, new_response.getMediaType());
    Person new_mariaUpdated = new_response.readEntity(Person.class);
    assertEquals(maria.getName(), new_mariaUpdated.getName());
    assertEquals(3, new_mariaUpdated.getId());
    assertEquals(mariaURI, new_mariaUpdated.getHref());
  }

  @Test
  public void listUsers() throws IOException {

    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    Person juan = new Person();
    juan.setName("Juan");
    ab.getPersonList().add(salvador);
    ab.getPersonList().add(juan);
    launchServer(ab);

    // Test list of contacts
    Client client = ClientBuilder.newClient();
    Response response = client.target("http://localhost:8282/contacts")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    AddressBook addressBookRetrieved = response
      .readEntity(AddressBook.class);
    assertEquals(2, addressBookRetrieved.getPersonList().size());
    assertEquals(juan.getName(), addressBookRetrieved.getPersonList()
      .get(1).getName());

    //////////////////////////////////////////////////////////////////////
    // Verify that GET /contacts is well implemented by the service, i.e
    // complete the test to ensure that it is safe and idempotent
    //////////////////////////////////////////////////////////////////////

    // Verify if the method is safe
    // ab object must has three persons on the person list (Salvador,juan,maria)
    assertEquals(2, ab.getPersonList().size());

    //Verify that the method is idempotent
    Response new_response = client.target("http://localhost:8282/contacts")
            .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    AddressBook new_addressBookRetrieved = new_response
            .readEntity(AddressBook.class);
    assertEquals(2, new_addressBookRetrieved.getPersonList().size());
    assertEquals(juan.getName(), new_addressBookRetrieved.getPersonList()
            .get(1).getName());

  }

  @Test
  public void updateUsers() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    salvador.setId(ab.nextId());
    Person juan = new Person();
    juan.setName("Juan");
    juan.setId(ab.getNextId());
    URI juanURI = URI.create("http://localhost:8282/contacts/person/2");
    ab.getPersonList().add(salvador);
    ab.getPersonList().add(juan);
    launchServer(ab);

    // Update Maria
    Person maria = new Person();
    maria.setName("Maria");
    Client client = ClientBuilder.newClient();
    Response response = client
      .target("http://localhost:8282/contacts/person/2")
      .request(MediaType.APPLICATION_JSON)
      .put(Entity.entity(maria, MediaType.APPLICATION_JSON));
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person juanUpdated = response.readEntity(Person.class);
    assertEquals(maria.getName(), juanUpdated.getName());
    assertEquals(2, juanUpdated.getId());
    assertEquals(juanURI, juanUpdated.getHref());

    // Verify that the update is real
    response = client.target("http://localhost:8282/contacts/person/2")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person mariaRetrieved = response.readEntity(Person.class);
    assertEquals(maria.getName(), mariaRetrieved.getName());
    assertEquals(2, mariaRetrieved.getId());
    assertEquals(juanURI, mariaRetrieved.getHref());

    // Verify that only can be updated existing values
    response = client.target("http://localhost:8282/contacts/person/3")
      .request(MediaType.APPLICATION_JSON)
      .put(Entity.entity(maria, MediaType.APPLICATION_JSON));
    assertEquals(400, response.getStatus());

    //////////////////////////////////////////////////////////////////////
    // Verify that PUT /contacts/person/2 is well implemented by the service, i.e
    // complete the test to ensure that it is idempotent but not safe
    //////////////////////////////////////////////////////////////////////

    // Verify that the method is idempotent
    // A new response must be the same as the previous response
    Response new_response = client
            .target("http://localhost:8282/contacts/person/2")
            .request(MediaType.APPLICATION_JSON)
            .put(Entity.entity(maria, MediaType.APPLICATION_JSON));
    assertEquals(200, new_response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, new_response.getMediaType());
    Person new_juanUpdated = new_response.readEntity(Person.class);
    assertEquals(new_juanUpdated.getName(), juanUpdated.getName());
    assertEquals(new_juanUpdated.getId(), juanUpdated.getId());
    assertEquals(new_juanUpdated.getHref(), juanUpdated.getHref());

    // Verify that the method is not secure
    // A new PUT call with other username, must modify the content of the object

    Person nerea = new Person();
    nerea.setName("Nerea");
    Response final_response = client
            .target("http://localhost:8282/contacts/person/2")
            .request(MediaType.APPLICATION_JSON)
            .put(Entity.entity(nerea, MediaType.APPLICATION_JSON));
    assertEquals(200, final_response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, final_response.getMediaType());
    Person nereaUpdated = final_response.readEntity(Person.class);
    assertEquals(nerea.getName(), nereaUpdated.getName());
    assertEquals(2, nereaUpdated.getId());
    assertEquals(juanURI, nereaUpdated.getHref());
  }

  @Test
  public void deleteUsers() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    salvador.setId(1);
    Person juan = new Person();
    juan.setName("Juan");
    juan.setId(2);
    ab.getPersonList().add(salvador);
    ab.getPersonList().add(juan);
    launchServer(ab);

    // Delete a user
    Client client = ClientBuilder.newClient();
    Response response = client
      .target("http://localhost:8282/contacts/person/2").request()
      .delete();
    assertEquals(204, response.getStatus());

    // Verify that the user has been deleted
    response = client.target("http://localhost:8282/contacts/person/2")
      .request().delete();
    assertEquals(404, response.getStatus());

    //////////////////////////////////////////////////////////////////////
    // Verify that DELETE /contacts/person/2 is well implemented by the service, i.e
    // complete the test to ensure that it is idempotent but not safe
    //////////////////////////////////////////////////////////////////////

    // Verify that the method is not secure
    // Check if there is only one person on the list
    assertEquals(1, ab.getPersonList().size());

    //Verify that the method is idempotent
    Response new_response = client.target("http://localhost:8282/contacts/person/2")
            .request().delete();
    assertEquals(404, new_response.getStatus());

    //////////////////////////////////////////////////////////////////////
    // BUG
    // This method is not idempotent at all because two concurrent calls could not
    // generate the same effect on the application.
    // For example, one of them can delete the user but the other one only returns 404
    // because the user doesn't exist
    //////////////////////////////////////////////////////////////////////
  }

  @Test
  public void findUsers() throws IOException {
    // Prepare server
    AddressBook ab = new AddressBook();
    Person salvador = new Person();
    salvador.setName("Salvador");
    salvador.setId(1);
    Person juan = new Person();
    juan.setName("Juan");
    juan.setId(2);
    ab.getPersonList().add(salvador);
    ab.getPersonList().add(juan);
    launchServer(ab);

    // Test user 1 exists
    Client client = ClientBuilder.newClient();
    Response response = client
      .target("http://localhost:8282/contacts/person/1")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    Person person = response.readEntity(Person.class);
    assertEquals(person.getName(), salvador.getName());
    assertEquals(person.getId(), salvador.getId());
    assertEquals(person.getHref(), salvador.getHref());

    // Test user 2 exists
    response = client.target("http://localhost:8282/contacts/person/2")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(200, response.getStatus());
    assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
    person = response.readEntity(Person.class);
    assertEquals(person.getName(), juan.getName());
    assertEquals(2, juan.getId());
    assertEquals(person.getHref(), juan.getHref());

    // Test user 3 exists
    response = client.target("http://localhost:8282/contacts/person/3")
      .request(MediaType.APPLICATION_JSON).get();
    assertEquals(404, response.getStatus());
  }

  private void launchServer(AddressBook ab) throws IOException {
    URI uri = UriBuilder.fromUri("http://localhost/").port(8282).build();
    server = GrizzlyHttpServerFactory.createHttpServer(uri,
      new ApplicationConfig(ab));
    server.start();
  }

  @After
  public void shutdown() {
    if (server != null) {
      server.shutdownNow();
    }
    server = null;
  }

}
