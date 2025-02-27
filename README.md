# Subotin-Motors-Portal

Veb aplikacija koja nudi tržište za kupovinu i prodaju polovnih automobila. Omogućava korisnicima da pregledaju širok izbor polovnih vozila i daju ponude za automobile za koje su zainteresovani, pružajući im potencijal da kupe vozilo po nižoj ceni.

## Pregled projekta

**Postoje 2 tipa korisnika na ovom portalu. I oni su**

1.  Krajni korisnici
2.  Administratori

**Korisnici su u mogućnosti da obavljaju sledeće funkcije na portalu**

1.  Registraciju na portal
2.  Prijavu na portal
3.  Postavljanje automobila na prodaju zajedno sa otpremanjem slike
4.  Deaktivaciju postojećeg oglasa
5.  Ažuriranje njihovog profila nakon prijave
6.  Zakazivanje probne vožnje
7.  Objavu ponudne cene

**Administratori su u mogućnosti da obavljaju sledeće funkcije na portalu**

1.  Registraciju na portal
2.  Prijavu na portal
3.  Pregled registrovanih korisnika
4.  Označavanje korisnika kao administratora
5.  Aktivaciju / Deaktivaciju oglasa
6.  Ažuriranje njihovog profila nakon prijave
7.  Odobreti / Odbiti termin test vožnje korisnika na osnovu licitacije
8.  Obavljanje transakcije ukoliko je odgovarajuća cena

**Korisnici i Administratori su u mogućnosti da obavljaju sledeće funkcije na portalu**

1.  Posećivanje početne stranice
2.  Pregled svih oglasa
3.  Potraga automobila na osnovu marke, modela, godišta registracije i raspona cena
4.  About Us Page
5.  Contact Us Page

## Korišćene tehnologije

Backend : Java SE 11, MySQL 8, Spring Boot, Spring Security
Frontend : JSP, JavaScript, Bootstrap

## Kako pokrenuti

1. **Import-ovati postojeći projekat u Visual Studio Code** 
2. **Kreirati MySQL bazu podataka**

```bash
mysql> CREATE DATABASE abc_cars;
```
2. **Podesite 'application propreties'**

```bash
spring.datasource.username=<YOUR_DB_USERNAME>
spring.datasource.password=<YOUR_DB_PASSWORD>
```

4. **Pokretanje aplikacije**

    U terminalu u Visual Studio Code, navigirajte do direktorijuma vašeg projekta:

    sh

```bash
cd C:\Users\David\Subotin-Motors-Portal
```
    Pokrećete aplikaciju koristeći Maven:

sh

```bash
    mvn spring-boot:run
```

Pristup aplikaciji:

    Otvorite web browser i idite na http://localhost:8080.
    Prijavite se koristeći sledeće kredencijale:
        Admin: admin123 / admin123
        User: user123 / user123

Brisanje Browser Cache-a (po potrebi):

    Ako imate problema sa prikazom starih podataka, obrišite keš pregledača za http://localhost:8080.



## Screenshot

<p>Home Page</p>
<img src="./images/home.png" alt="home_page" width="50%"/>
<p>Login</p>
<img src="./images/login.png" alt="login" width="50%"/>
<p>Profile page</p>
<img src="./images/profile.png" alt="profile" width="50%"/>
<p>Cars Page</p>
<img src="./images/cars.png" alt="cars" width="50%"/>
<p>Car Detail Page</p>
<img src="./images/car-detail.png" alt="car_detail" width="50%"/>
<p>Post Car</p>
<img src="./images/post-car.png" alt="post_car" width="50%"/>
<p>Bid Car</p>
<img src="./images/place-bid.png" alt="bid_car" width="50%"/>
<p>Test Drive</p>
<img src="./images/test-drive.png" alt="test_drive" width="50%"/>
<p>Appointment</p>
<img src="./images/appointment.png" alt="appointment" width="50%"/>
<p>My Posted Car</p>
<img src="./images/my-posted-car.png" alt="my_posted_car" width="50%"/>
<p>About Page</p>
<img src="./images/about.png" alt="about" width="50%"/>
<p>Contact Page</p>
<img src="./images/contact.png" alt="contact" width="50%"/>
<p>Admin Pages</p>
<img src="./images/admin.png" alt="admin" width="50%"/>
<img src="./images/admin2.png" alt="admin" width="50%"/>
