# Projektna dokumentacija: Autostrada Auctions

Predmet: Eksploatacija, održavanje i nadogradnja informacionih sistema  
Student: David Subotin IT-40/2022  
GitHub repozitorijum: https://github.com/DavidSubotinDS/SubotinMotors  
Tip projekta: veb aplikacija za tržište automobila, aukcije i prodavnicu auto-delova  
Verzija: jun 2026.

## Sadržaj

1. Opis realnog sistema  
2. Korišćene tehnologije  
3. Slučajevi upotrebe  
4. Model domena i relacije  
5. Baza podataka  
6. Opis predloženog rešenja  
7. Bezbednost, validacija i poslovna pravila  
8. Demonstracioni podaci i lokalno pokretanje  
9. Testiranje i obezbeđenje kvaliteta  
10. GitHub repozitorijum  
11. Ograničenja i mogućnosti daljeg unapređenja  
12. Literatura i reference  
13. Zaključak

## Provera spremnosti aplikacije

Aplikacija je proverena u odnosu na obaveštenje o predaji projektne dokumentacije, priložene smernice za pisanje i primer projektne dokumentacije. Dostupni dokazi u repozitorijumu pokazuju da je projekat spreman za odbranu: izvorni kod sadrži glavne tokove rada tržišta, dokumentacija sadrži link do GitHub repozitorijuma, a automatizovani testovi prolaze uspešno.

| Zahtev | Status | Dokaz |
| --- | --- | --- |
| Kompletiran projekat za odbranu | Ispunjeno | Aplikacija sadrži javne, korisničke i administratorske tokove rada za naloge, automobile, aukcije, oglase sa fiksnom cenom, test-vožnje, auto-delove, korpe, porudžbine i plaćanja. |
| Projektna dokumentacija | Ispunjeno ovim dokumentom | Dokumentacija opisuje realni sistem, tehnologije, slučajeve upotrebe, model domena, bazu podataka, arhitekturu, bezbednost, validaciju, testiranje i lokalno pokretanje. |
| Link do GitHub repozitorijuma | Ispunjeno | Link do repozitorijuma naveden je na početku dokumenta i u posebnom poglavlju. |
| Zahtevi projektne specifikacije | Ispunjeno prema dokazima iz repozitorijuma | Izvorni kod i dokument `docs/crud-coverage.md` pokrivaju CRUD i životne cikluse, autorizaciju po ulogama, moderaciju, validaciju, obradu plaćanja i očuvanje istorije. |
| Kvalitet i provera | Ispunjeno | Komanda `.\mvnw.cmd test` izvršena je uspešno: 75 testova, 0 neuspešnih testova i 0 grešaka. |
| Smernice za dokumentaciju | Ispunjeno | Dokument koristi formalan stil, strukturirane naslove, zvanične reference i ne oslanja se na nepouzdane izvore. |

## 1. Opis realnog sistema

U savremenom poslovanju kupovina i prodaja polovnih vozila sve češće se oslanjaju na digitalne platforme. Tradicionalni način prodaje automobila često podrazumeva nepovezane oglase, ručnu komunikaciju između kupca i prodavca, nejasan status dostupnosti vozila i otežano praćenje dogovora, ponuda i zakazanih pregleda. Zbog toga postoji potreba za informacionim sistemom koji objedinjeno podržava objavljivanje vozila, pretragu, aukcijsku prodaju, komunikaciju sa kupcima, zakazivanje test-vožnji i administratorski nadzor.

Predmet ovog projekta jeste razvoj veb aplikacije Autostrada Auctions, koja predstavlja digitalno tržište za automobile i auto-delove. Sistem omogućava javno pregledanje aktivnih vozila, registraciju korisnika, postavljanje automobila na prodaju, davanje ponuda na aukcijama, zakazivanje test-vožnji, praćenje aukcija, kupovinu auto-delova kroz prodavnicu i pregled istorije aktivnosti. Pored toga, administratori imaju poseban deo sistema za upravljanje korisnicima, odobravanje oglasa, upravljanje zalihama auto-delova, pregled porudžbina i kontrolu transakcija.

Sa aspekta realnog sistema, aplikacija povezuje nekoliko poslovnih procesa:

- proces registracije i identifikacije korisnika;
- proces objavljivanja i moderacije automobila;
- proces aukcijske prodaje i evidencije ponuda;
- proces zakazivanja i odobravanja test-vožnji;
- proces pretrage i kupovine auto-delova;
- proces rezervacije zaliha i potvrde plaćanja;
- proces administratorskog nadzora i očuvanja poslovne istorije.

Poseban značaj ima razdvajanje javnih, korisničkih i administratorskih funkcionalnosti. Posetioci mogu da pregledaju sadržaj, dok se osetljive operacije, poput davanja ponuda, upravljanja korpom, zakazivanja test-vožnji i uređivanja profila, omogućavaju samo prijavljenim korisnicima. Administratorske funkcionalnosti dostupne su isključivo korisnicima sa odgovarajućom ulogom.

Sistem je projektovan tako da se važni poslovni zapisi ne brišu proizvoljno. Ponude, porudžbine, plaćanja, test-vožnje i revizioni zapisi čuvaju se kroz statuse i istoriju, jer su potrebni za praćenje toka prodaje i kasniju proveru. Time aplikacija ne predstavlja samo skup obrazaca za unos podataka, već informacioni sistem sa definisanim životnim ciklusima i pravilima integriteta.

## 2. Korišćene tehnologije

Za realizaciju aplikacije korišćen je tehnološki stek zasnovan na Java i Spring ekosistemu. Arhitektura je prilagođena serverski renderovanoj veb aplikaciji sa jasnom podelom na kontrolere, servise, repozitorijume, entitete i JSP prikaze.

### 2.1 Prezentacioni sloj

Korisnički interfejs realizovan je pomoću JSP stranica, JSTL tagova, Spring Security tagova, Bootstrap biblioteke, prilagođenih CSS datoteka i manjih JavaScript modula. JSP prikazi nalaze se u direktorijumu `src/main/webapp/view`, dok se statički resursi nalaze u `src/main/resources/static`.

Bootstrap se koristi za osnovnu responzivnost i dosledan prikaz elemenata interfejsa. Prilagođene CSS datoteke dodatno oblikuju stranice za početnu stranu, katalog automobila, detalje automobila, administratorski panel, prodavnicu auto-delova, korisnički profil i forme. JavaScript se koristi za elemente kao što su odbrojavanje trajanja aukcija, pregled slika uz komentare i interakcije pri zakazivanju test-vožnje.

### 2.2 Serverski deo aplikacije

Serverski deo implementiran je pomoću Java 17 i Spring Boot 3.5. Aplikacija koristi Spring MVC za obradu HTTP zahteva, povezivanje formi i vraćanje JSP prikaza. Poslovna logika izdvojena je u servisni sloj, dok se pristup podacima realizuje kroz Spring Data JPA repozitorijume.

Kontroleri obrađuju javne, korisničke i administratorske rute. Servisi sadrže pravila kao što su provera vlasništva, promena statusa ponuda, provera aukcijskih rokova, odobravanje test-vožnji, rezervacija zaliha i obrada događaja plaćanja. Ovakva podela omogućava da se poslovna logika ne nalazi direktno u prikazima, već u delovima sistema koji se mogu testirati.

### 2.3 Baza podataka i migracije

Za lokalni razvoj i testiranje koristi se H2 baza podataka, dok je za okruženje slično produkcionom dostupan MySQL profil. Struktura baze se razvija kroz Flyway SQL migracije koje se nalaze u direktorijumu `src/main/resources/db/migration`.

Za mapiranje objekata na relacione tabele koristi se Spring Data JPA, odnosno Hibernate kao JPA implementacija. Ovakav pristup omogućava rad sa entitetima u aplikativnom kodu, dok Flyway obezbeđuje kontrolisano verzionisanje šeme baze podataka.

### 2.4 Autentifikacija i autorizacija

Autentifikacija i autorizacija implementirane su pomoću Spring Security. Korisnici se prijavljuju korisničkim imenom i lozinkom, a lozinke se čuvaju uz BCrypt heširanje. Uloge korisnika predstavljene su kroz entitet `Role`, pri čemu se razlikuju obični korisnici i administratori.

Sistem razlikuje javne rute, korisničke rute i administratorske rute. Rute pod `/user/**` zahtevaju prijavljenog korisnika, dok rute pod `/admin/**` zahtevaju administratorsku ulogu. Dodatne provere vlasništva sprovode se u servisnom sloju.

### 2.5 Plaćanje

Plaćanje u prodavnici auto-delova realizovano je kroz Stripe probno okruženje. Integracija je ograničena na testne Stripe ključeve i ne prihvata produkcione ključeve. Kupovina auto-delova pokreće Stripe Checkout sesiju, dok se porudžbina označava kao plaćena tek nakon provere potpisanog *webhook* događaja.

Ovakav pristup sprečava da se status porudžbine promeni samo na osnovu preusmerenja iz pregledača. Izvor istine za završetak plaćanja je potpisani događaj pružaoca plaćanja.

### 2.6 Dodatne biblioteke i alati

Projekat koristi Maven Wrapper za ponovljivo pokretanje izgradnje i testova. Za testiranje se koriste JUnit, Spring Boot Test, MockMvc i Spring Security Test. Jakarta Bean Validation koristi se za validaciju formi i entiteta. Flyway obezbeđuje migracije, a Spring Mail podržava log ili SMTP isporuku poruka za oporavak lozinke.

## 3. Slučajevi upotrebe

Slučajevi upotrebe prikazuju kako glavni akteri koriste sistem. U sistemu postoje četiri osnovne grupe aktera: posetilac, registrovani korisnik, prodavac i administrator. Pored njih, eksterni sistem Stripe učestvuje u obradi plaćanja, dok pozadinski servis učestvuje u kreiranju obaveštenja.

### 3.1 Tekstualni opis slučajeva upotrebe

| Br. | Slučaj upotrebe | Akteri | Opis i ishod |
| --- | --- | --- | --- |
| 1 | Registracija naloga | Posetilac | Posetilac unosi podatke naloga i profila. Sistem proverava jedinstvenost korisničkog imena i adrese elektronske pošte, zatim kreira korisnički nalog sa osnovnom korisničkom ulogom. |
| 2 | Prijava u sistem | Korisnik, administrator | Korisnik unosi korisničko ime i lozinku. Sistem proverava kredencijale i usmerava korisnika na odgovarajući deo aplikacije. |
| 3 | Oporavak lozinke | Korisnik | Korisnik zahteva link za resetovanje lozinke. Sistem kreira heširani token sa rokom trajanja i omogućava promenu lozinke ako je token validan. |
| 4 | Pregled javnog kataloga automobila | Posetilac | Posetilac pregleda aktivne automobile i detalje vozila. Automobili koji čekaju odobrenje ili nisu aktivni ne prikazuju se u javnom katalogu. |
| 5 | Pretraga, filtriranje i sortiranje automobila | Posetilac | Posetilac pretražuje automobile po ključnoj reči, ceni, straničenju i sortiranju. Sistem odbija neispravan opseg cena. |
| 6 | Postavljanje automobila na aukciju | Prodavac | Prijavljeni korisnik unosi podatke o automobilu i sliku. Novi automobil dobija status na čekanju i zahteva administratorsko odobrenje. |
| 7 | Odobravanje ili odbijanje automobila | Administrator | Administrator pregleda automobile koji čekaju proveru i može ih odobriti ili odbiti. Odobren automobil postaje dostupan u javnom toku rada. |
| 8 | Izmena sopstvenog automobila | Prodavac | Vlasnik oglasa može menjati podatke i slike sopstvenog automobila. Sistem ne dozvoljava izmenu automobila drugog korisnika. |
| 9 | Davanje ponude na aukciji | Korisnik | Korisnik daje ponudu za aktivan automobil. Sistem proverava da automobil nije njegov, da je aktivan i da aukcija nije istekla. |
| 10 | Otkazivanje sopstvene ponude | Korisnik | Korisnik može otkazati samo svoju ponudu koja je još u toku. Otkazana ponuda ostaje u istoriji. |
| 11 | Prihvatanje ili odbijanje ponude | Administrator | Administrator može prihvatiti pobedničku ponudu, odbiti ostale ponude i promeniti status automobila u skladu sa prodajnim tokom. |
| 12 | Zakazivanje test-vožnje | Korisnik | Korisnik bira budući datum test-vožnje za automobil. Sistem sprečava zakazivanje u prošlosti i duplirane termine za istog korisnika, automobil i datum. |
| 13 | Upravljanje zahtevima za test-vožnju | Prodavac | Vlasnik automobila može prihvatiti, odbiti ili otkazati zahtev za test-vožnju. Podnosilac zahteva može promeniti termin ili otkazati zahtev. |
| 14 | Praćenje aukcije | Korisnik | Korisnik može pratiti aktivnu aukciju i kasnije je videti u listi praćenih aukcija. Duplirano praćenje je sprečeno. |
| 15 | Pregled obaveštenja | Korisnik | Korisnik pregleda obaveštenja o aukcijama koje se uskoro završavaju i može ih označiti kao pročitana. |
| 16 | Pregled oglasa sa fiksnom cenom | Posetilac | Posetilac pregleda oglase vozila koja se prodaju po fiksnoj ceni, uključujući osnovne podatke, cenu i opis. |
| 17 | Zakazivanje test-vožnje za oglas sa fiksnom cenom | Korisnik | Korisnik može zahtevati termin za probnu vožnju vozila iz dela sa fiksnom cenom. Prodavac upravlja zahtevom kroz statuse. |
| 18 | Plaćanje depozita za listing | Korisnik, Stripe | Korisnik pokreće plaćanje depozita za određeni listing. Sistem kreira evidenciju depozita i koristi Stripe probno okruženje za plaćanje. |
| 19 | Pregled prodavnice auto-delova | Posetilac | Posetilac pregleda dostupne auto-delove, kategorije, cene, stanje zaliha i detalje proizvoda. |
| 20 | Upravljanje korpom | Korisnik | Korisnik dodaje delove u korpu, menja količine ili uklanja stavke. Sistem poštuje ograničenja dostupnih zaliha. |
| 21 | Plaćanje porudžbine u prodavnici | Korisnik, Stripe | Korpa se pretvara u porudžbinu, zalihe se rezervišu i korisnik se preusmerava na Stripe Checkout sesiju. |
| 22 | Potvrda plaćanja putem *webhook* događaja | Stripe, sistem | Stripe šalje potpisani događaj. Sistem proverava potpis, obrađuje događaj idempotentno i ažurira status porudžbine. |
| 23 | Pregled istorije porudžbina | Korisnik | Korisnik vidi samo sopstvene porudžbine i detalje kupljenih stavki. |
| 24 | Upravljanje proizvodima prodavnice | Administrator | Administrator dodaje i menja auto-delove, cene, kategorije, zalihe, vidljivost i slike. Proizvodi se deaktiviraju umesto proizvoljnog brisanja kada je potrebno očuvati istoriju. |
| 25 | Pregled porudžbina prodavnice | Administrator | Administrator pregleda porudžbine korisnika, njihove statuse, ukupne iznose, stavke i podatke o plaćanju. |
| 26 | Pregled transakcija | Administrator | Administrator pregleda istorijske zapise plaćanja, identifikatore pružaoca plaćanja i *webhook* događaje. |
| 27 | Komentarisanje oglasa i delova | Korisnik | Prijavljeni korisnik može ostaviti komentar na automobil ili auto-deo. Slike uz komentare se validiraju pre čuvanja. |
| 28 | Izmena profila i slike profila | Korisnik | Korisnik menja lične podatke, adresu, broj telefona, opis i sliku profila. Slike prolaze proveru tipa i sadržaja. |
| 29 | Administratorsko upravljanje korisnicima | Administrator | Administrator pregleda korisnike, uređuje profile i može dodeliti administratorsku ulogu. |
| 30 | Odjava iz sistema | Korisnik, administrator | Korisnik završava sesiju, nakon čega zaštićene funkcionalnosti ponovo zahtevaju prijavu. |

## 4. Model domena i relacije

Model domena zasnovan je na entitetima koji predstavljaju korisnike, vozila, oglase, ponude, test-vožnje, auto-delove, porudžbine i plaćanja. Entiteti su povezani relacijama koje odražavaju poslovnu logiku tržišta.

### 4.1 Tekstualni opis glavnih klasa

`UserAccount` predstavlja registrovanog korisnika sistema. Sadrži korisničko ime, jedinstvenu adresu elektronske pošte, heširanu lozinku, profil, uloge, automobile i ponude. Jedan korisnik može imati više uloga, više automobila, više ponuda, više stavki u korpi i više porudžbina.

`UserProfile` predstavlja lične podatke korisnika, uključujući ime, prezime, broj telefona, adresu, grad, poštanski broj, državu i opis profila. Profil je povezan sa nalogom u odnosu jedan-prema-jedan. `ProfilePicture` predstavlja sliku profila i povezana je sa profilom.

`Role` predstavlja ulogu korisnika, na primer `ROLE_USER` ili `ROLE_ADMIN`. Relacija između korisnika i uloga omogućava autorizaciju zasnovanu na ulogama.

`Car` predstavlja automobil koji se prodaje kroz aukcijski tok. Sadrži marku, model, godinu proizvodnje, status, cenu, vreme završetka aukcije i vlasnika. Automobil može imati sliku, ponude, test-vožnje, komentare, praćenja i obaveštenja.

`CarBidding` predstavlja ponudu na aukciji. Povezana je sa korisnikom koji daje ponudu i automobilom na koji se ponuda odnosi. Ponuda čuva cenu i status, na primer u toku, prihvaćena, odbijena ili otkazana.

`TestDrive` predstavlja zahtev za test-vožnju aukcijskog automobila. Povezuje korisnika, automobil, datum i status zahteva. Statusi obuhvataju čekanje, prihvatanje, odbijanje i otkazivanje.

`CarListing` predstavlja oglas sa fiksnom cenom. Sadrži naslov, marku, model, godinu proizvodnje, kilometražu, tip goriva, menjač, cenu, iznos depozita, opis, status i prodavca. Povezan je sa slikom, test-vožnjama za listing i depozitima.

`CarPart` predstavlja auto-deo u prodavnici. Sadrži šifru artikla, naziv, kategoriju, opis, cenu u manjim novčanim jedinicama, količinu na stanju, aktivnost i opcionu adresu slike.

`CartItem` predstavlja stavku korisničke korpe. Povezuje korisnika, auto-deo i količinu. Dodavanje istog proizvoda više puta objedinjuje se u postojeću stavku.

`StoreOrder` predstavlja porudžbinu iz prodavnice auto-delova. Sadrži korisnika, ukupan iznos, valutu, status, podatke o adresi isporuke, identifikator Stripe Checkout sesije, identifikator plaćanja i vreme plaćanja. `StoreOrderItem` čuva stavke porudžbine, uključujući naziv, šifru, cenu i količinu u trenutku kupovine.

`PaymentOrder` predstavlja istorijske ili aukcijske zapise plaćanja. Povezuje ponudu, kupca, prodavca, iznos, proviziju, valutu, status, namenu, Stripe identifikatore i vreme plaćanja.

`PaymentWebhookEvent` predstavlja revizioni zapis obrađenog *webhook* događaja. Jedinstveni identifikator događaja sprečava duplu obradu istog događaja.

`PasswordResetToken` predstavlja token za oporavak lozinke. U bazi se čuva heš tokena, vreme isteka i informacija o tome da li je token iskorišćen.

`ListingComment` predstavlja komentar na automobil ili auto-deo. Sadrži autora, tekst komentara, vreme kreiranja i opcionu sliku.

### 4.2 Relacije između entiteta

Najvažnije relacije u sistemu su:

- Jedan korisnik ima jedan profil, a jedan profil pripada jednom korisniku.
- Jedan korisnik može imati više uloga.
- Jedan korisnik može postaviti više automobila i više oglasa sa fiksnom cenom.
- Jedan automobil pripada jednom vlasniku, ali može imati više ponuda, test-vožnji, komentara, praćenja i obaveštenja.
- Jedna ponuda pripada jednom korisniku i jednom automobilu.
- Jedna test-vožnja pripada jednom korisniku i jednom automobilu ili oglasu, u zavisnosti od toka rada.
- Jedan auto-deo može se pojaviti u više stavki korpe i više stavki porudžbina.
- Jedna porudžbina pripada jednom korisniku i sadrži više stavki porudžbine.
- Jedan Stripe događaj se čuva jednom u tabeli *webhook* događaja, čime se postiže idempotentnost.

Ovakav model omogućava jasnu povezanost poslovnih objekata i sprečava gubitak istorije. Na primer, stavke porudžbine čuvaju naziv i cenu proizvoda iz trenutka kupovine, čak i ako se proizvod kasnije promeni u katalogu.

## 5. Baza podataka

Aplikacija koristi relacione baze podataka. U lokalnom režimu koristi se H2, dok se za MySQL može uključiti poseban profil. Struktura baze se ne kreira ručno pri svakom pokretanju, već se kontroliše kroz Flyway migracije. Svaka migracija ima svoju verziju i opis, čime se omogućava ponovljivo kreiranje i nadogradnja šeme.

### 5.1 Pristup razvoju baze

U projektu je primenjen pristup zasnovan na migracijama. SQL migracije definišu strukturu tabela, indekse, ograničenja i demonstracione podatke, dok JPA entiteti mapiraju aplikativni model na te tabele. Poslovna logika nije implementirana kroz trigere baze, već kroz servisni sloj aplikacije.

Prednost ovog pristupa jeste jasna kontrola promena nad bazom. Prilikom pokretanja aplikacije Flyway proverava primenjene migracije i izvršava one koje nedostaju. Testovi koriste posebnu H2 bazu u memoriji i tako proveravaju da migracije mogu da izgrade kompletnu šemu od početka.

### 5.2 Struktura baze podataka

Ključne grupe tabela su:

- Korisničke tabele: korisnički nalozi, uloge, profili, slike profila i tokeni za oporavak lozinke.
- Aukcijske tabele: automobili, slike automobila, ponude, test-vožnje, praćenja i obaveštenja.
- Tabele oglasa sa fiksnom cenom: oglasi, slike oglasa, test-vožnje za oglase i depoziti.
- Prodavnica: auto-delovi, stavke korpe, porudžbine i stavke porudžbina.
- Plaćanja: nalozi plaćanja, Stripe identifikatori, događaji plaćanja i reviziona evidencija.
- Komentari: komentari nad automobilima i auto-delovima, uključujući opcione slike.

### 5.3 CRUD i životni ciklusi

Spring Data repozitorijumi obezbeđuju osnovne operacije nad bazom, ali se one ne izlažu direktno korisniku. Kontroleri pozivaju servise koji proveravaju uloge, vlasništvo i važeće prelaze stanja.

| Oblast | Kreiranje | Pregled | Izmena | Brisanje ili uklanjanje iz aktivnog toka |
| --- | --- | --- | --- | --- |
| Korisnici i profili | Javna registracija u dva koraka | Korisnik vidi svoj profil; administrator vidi listu korisnika | Korisnik menja svoj profil; administrator može menjati profil i dodeliti ulogu | Trajno brisanje nije izloženo jer korisnik može biti povezan sa ponudama, porudžbinama i plaćanjima. |
| Automobili | Prodavac kreira automobil u stanju čekanja | Javni katalog prikazuje samo dozvoljene statuse; vlasnik i administrator vide šire podatke | Vlasnik menja svoj automobil; administrator odobrava ili odbija | Deaktivacija se koristi kao poslovno uklanjanje iz javnog toka. |
| Ponude | Korisnik daje ponudu na aktivnoj aukciji | Korisnik vidi svoje ponude; administrator vidi ponude u upravljanju automobilima | Administrator prihvata ili odbija; korisnik može otkazati svoju ponudu u toku | Ponude se ne brišu jer čuvaju istoriju aukcije. |
| Test-vožnje | Korisnik šalje zahtev za budući datum | Korisnik i vlasnik automobila vide relevantne zahteve | Vlasnik prihvata ili odbija; korisnik menja termin | Otkazivanje menja status u otkazano, a istorija ostaje sačuvana. |
| Auto-delovi | Administrator dodaje proizvod | Svi korisnici vide aktivne proizvode; administrator vidi upravljački pregled | Administrator menja cenu, zalihu, kategoriju i vidljivost | Proizvod se deaktivira umesto brisanja kada postoji istorija porudžbina. |
| Korpa | Korisnik dodaje proizvod u korpu | Korisnik vidi samo svoju korpu | Korisnik menja količinu | Korisnik uklanja stavku; uspešno plaćanje prazni korpu. |
| Porudžbine i plaćanja | Plaćanje kreira porudžbinu i Stripe sesiju | Korisnik vidi svoje porudžbine; administrator vidi sve | Potpisani *webhook* menja status porudžbine | Finansijski zapisi se ne brišu proizvoljno. |

## 6. Opis predloženog rešenja

Predloženo rešenje predstavlja serverski renderovanu veb aplikaciju koja objedinjuje tržište automobila, aukcijske tokove, oglase sa fiksnom cenom, prodavnicu auto-delova i administratorski panel. Sistem je prilagođen demonstraciji realnih poslovnih procesa, ali ostaje bezbedan za školski projekat jer su plaćanja ograničena na probno Stripe okruženje.

### 6.1 Arhitektura sistema

Aplikacija je organizovana kroz nekoliko slojeva:

- prezentacioni sloj, koji čine JSP stranice i statički resursi;
- kontrolerski sloj, koji obrađuje rute i forme;
- servisni sloj, koji sadrži poslovna pravila;
- repozitorijumski sloj, koji obezbeđuje rad sa bazom;
- sloj entiteta i DTO klasa, koji modeluje trajne podatke i korisnički unos;
- integracioni sloj za Stripe, elektronsku poštu i zakazane obrade.

Ovakva podela omogućava jasnu separaciju odgovornosti. Kontroleri ne odlučuju sami o poslovnom stanju, već prosleđuju zahteve servisima. Servisi proveravaju prava pristupa, statuse, vlasništvo i integritet podataka.

### 6.2 Korisnički deo sistema

Korisnički deo sistema omogućava sledeće aktivnosti:

- registraciju, prijavu i oporavak lozinke;
- pregled i izmenu profila;
- postavljanje i uređivanje sopstvenih automobila;
- pregled javnog kataloga automobila i oglasa sa fiksnom cenom;
- davanje i otkazivanje ponuda;
- zakazivanje, promenu termina i otkazivanje test-vožnji;
- praćenje aukcija i pregled obaveštenja;
- komentarisanje automobila i auto-delova;
- pregled prodavnice auto-delova;
- upravljanje korpom;
- pokretanje plaćanja u prodavnici;
- pregled istorije porudžbina.

Korisnički tokovi su osmišljeni tako da korisnik vidi samo sopstvene osetljive podatke. Na primer, istorija porudžbina se vezuje za trenutno prijavljenog korisnika, a izmena automobila zahteva vlasništvo nad tim automobilom.

### 6.3 Administrativni deo sistema

Administrativni deo sistema obuhvata:

- pregled korisnika;
- izmenu korisničkih profila;
- dodeljivanje administratorske uloge;
- pregled i moderaciju automobila;
- odobravanje ili odbijanje pending oglasa;
- prihvatanje i odbijanje ponuda;
- upravljanje auto-delovima;
- pregled porudžbina prodavnice;
- pregled istorijskih transakcija i *webhook* događaja.

Administratorski panel je zaštićen pravilima autorizacije. Obični korisnici i anonimni posetioci ne mogu pristupiti ovim rutama.

### 6.4 Integracija sa Stripe sistemom za plaćanje

Integracija sa Stripe sistemom koristi se za probno plaćanje porudžbina iz prodavnice auto-delova. Proces plaćanja obuhvata sledeće korake:

1. Korisnik dodaje auto-delove u korpu.
2. Sistem proverava zalihe i kreira porudžbinu.
3. Zalihe se rezervišu dok je plaćanje u toku.
4. Kreira se Stripe Checkout sesija.
5. Korisnik završava plaćanje u Stripe probnom okruženju.
6. Stripe šalje potpisani *webhook* događaj aplikaciji.
7. Sistem proverava potpis, obrađuje događaj i menja status porudžbine.
8. Ako plaćanje istekne ili ne uspe, rezervisane zalihe se vraćaju.

Ovakav tok je važan jer sprečava da porudžbina bude označena kao plaćena samo zato što je korisnik vraćen na stranicu uspeha. Plaćanje se potvrđuje tek nakon događaja iz eksternog sistema.

### 6.5 Upravljanje stanjem sistema

Aplikacija koristi poslovne statuse da bi očuvala istoriju i sprečila nevalidne operacije. Automobili mogu biti aktivni, neaktivni, na čekanju, rezervisani ili prodati. Ponude mogu biti u toku, prihvaćene, odbijene ili otkazane. Test-vožnje imaju statuse na čekanju, prihvaćeno, odbijeno i otkazano. Porudžbine i plaćanja imaju statuse koji zavise od Stripe događaja.

Ovakvo upravljanje stanjima omogućava da se podaci ne brišu čim više nisu aktivni. Umesto toga, zapis ostaje u sistemu sa novim statusom, što je korisno za proveru i istoriju poslovanja.

### 6.6 Korisnički interfejs i iskustvo

Korisnički interfejs je podeljen na javni deo, korisnički nalog i administratorski deo. Javni deo omogućava brzo pregledanje automobila, oglasa i auto-delova. Korisnički deo sadrži profil, objavljene automobile, ponude, test-vožnje, praćene aukcije, obaveštenja, korpu i porudžbine. Administratorski deo koristi tabele i forme za pregled većeg broja zapisa i efikasno upravljanje sistemom.

Posebna pažnja posvećena je jasnom prikazu stanja. Aukcije prikazuju odbrojavanje, automobili koji čekaju odobrenje imaju poseban status, test-vožnje prikazuju trenutni tok zahteva, a porudžbine prikazuju status plaćanja. Time korisnik lakše razume šta je sledeći korak u poslovnom procesu.

## 7. Bezbednost, validacija i poslovna pravila

Bezbednost i validacija nisu ograničene samo na prikaz grešaka u formama. Važna pravila implementirana su u entitetima, DTO klasama, servisima i konfiguraciji bezbednosti.

Ključna bezbednosna pravila su:

- javne stranice dostupne su bez prijave;
- korisničke stranice zahtevaju autentifikovanog korisnika;
- administratorske stranice zahtevaju administratorsku ulogu;
- korisnik ne može uređivati automobil koji ne poseduje;
- korisnik ne može otkazati tuđu ponudu;
- korisnik ne može upravljati tuđom test-vožnjom;
- vlasnik automobila ne može davati ponudu za sopstveni automobil;
- Stripe produkcioni ključevi se odbijaju jer je projekat ograničen na probno okruženje;
- plaćanje se potvrđuje samo na osnovu verifikovanog *webhook* događaja.

Validaciona pravila uključuju:

- godina proizvodnje mora biti realna i ne sme biti u budućnosti;
- broj telefona mora odgovarati prihvaćenom međunarodnom ili nacionalnom formatu;
- cena ponude mora biti pozitivna;
- datum test-vožnje mora biti u budućnosti;
- minimalna cena u pretrazi ne sme biti veća od maksimalne;
- adresa elektronske pošte je obavezna, validna i jedinstvena;
- postavljene slike moraju imati dozvoljeni MIME tip, odgovarajući potpis datoteke i sadržaj koji se može pročitati kao slika.

Poslovna pravila koja obezbeđuju integritet sistema su:

- novi automobili čekaju administratorsko odobrenje pre javnog prikaza;
- automobili koji nisu aktivni ne prikazuju se u javnom katalogu;
- aukcijske ponude se odbijaju nakon isteka aukcije;
- prihvatanje pobedničke ponude utiče na status automobila i ostalih ponuda;
- test-vožnja promenjena nakon prihvatanja vraća se u stanje čekanja;
- prodavnica ponovo proverava zalihe prilikom pokretanja plaćanja;
- neuspešno ili isteklo plaćanje vraća rezervisane zalihe;
- *webhook* događaji se obrađuju idempotentno pomoću jedinstvenog identifikatora događaja.

## 8. Demonstracioni podaci i lokalno pokretanje

Podrazumevana konfiguracija koristi H2 bazu podataka, pa nije potrebna posebna instalacija baze za lokalnu demonstraciju. Pri pokretanju aplikacije Flyway primenjuje migracije i unosi demonstracione podatke.

Demonstracioni podaci pokrivaju različite scenarije:

- administratorski nalog;
- osnovni korisnički nalog;
- korisnika koji se primarno ponaša kao kupac;
- korisnika koji se primarno ponaša kao prodavac;
- korisnika sa istorijom kupovine i prodaje;
- korisnika bez istorije aktivnosti;
- aktivne aukcije;
- automobile koji čekaju odobrenje;
- neaktivne, rezervisane i prodate automobile;
- ponude i istoriju plaćanja;
- test-vožnje u različitim statusima;
- praćene aukcije i obaveštenja;
- proizvode prodavnice auto-delova;
- porudžbine i komentare.

Dokumentovani demonstracioni nalozi su:

- `admin123` / `admin123`
- `user123` / `user123`
- `demo_bidder` / `demo123`
- `demo_seller` / `demo123`
- `demo_trader` / `demo123`
- `demo_newcomer` / `demo123`

Za lokalno pokretanje potreban je Java 17 ili novija verzija. Aplikacija se pokreće komandom:

```powershell
.\mvnw.cmd spring-boot:run
```

Nakon pokretanja aplikacija je dostupna na adresi:

```text
http://localhost:8080
```

Za pokretanje testova koristi se:

```powershell
.\mvnw.cmd test
```

Za pakovanje aplikacije koristi se:

```powershell
.\mvnw.cmd clean package
```

Za MySQL režim rada potrebno je podesiti profil `mysql` i promenljive okruženja za konekciju sa bazom. Stripe plaćanje ostaje ograničeno na probno okruženje i zahteva testne kredencijale ako se demonstrira plaćanje.

## 9. Testiranje i obezbeđenje kvaliteta

Projekat sadrži automatizovane testove za najvažnije tokove rada i poslovna pravila. Testovi koriste zasebnu H2 bazu u memoriji, što omogućava proveru migracija i poslovnih tokova bez oslanjanja na lokalnu razvojnu bazu.

Kompletan Maven skup testova izvršen je uspešno:

```powershell
.\mvnw.cmd test
```

Rezultat izvršavanja:

```text
Tests run: 75, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

Testovi pokrivaju:

- učitavanje aplikacionog konteksta;
- javne i zaštićene rute;
- registraciju i duplirana korisnička imena;
- jedinstvenost i validaciju adrese elektronske pošte;
- autentifikaciju demonstracionih korisnika;
- pretragu, straničenje i sortiranje kataloga;
- odbijanje neispravnog opsega cena;
- administratorsku autorizaciju;
- validacione greške bez upisa nevalidnih podataka;
- tok rada test-vožnji;
- vlasničke provere nad automobilima, ponudama i test-vožnjama;
- aukcijske rokove i statuse;
- praćenje aukcija i obaveštenja;
- komentare i validaciju slika;
- demonstracione podatke;
- prodavnicu auto-delova;
- rezervaciju zaliha;
- Stripe probne kredencijale;
- idempotentnu obradu *webhook* događaja;
- prikaz administratorskih transakcija.

Na osnovu uspešnog izvršavanja testova može se zaključiti da su ključni tokovi rada provereni automatizovano i da aplikacija ima osnovu za pouzdanu demonstraciju.

## 10. GitHub repozitorijum

Kompletan projekat postavljen je na GitHub platformu. Repozitorijum sadrži izvorni kod aplikacije, JSP prikaze, statičke resurse, konfiguracione datoteke, Flyway migracije, testove, dokumentaciju i skripte za pomoć pri lokalnoj demonstraciji Stripe probnog okruženja.

Link ka GitHub repozitorijumu:

```text
https://github.com/DavidSubotinDS/SubotinMotors
```

U repozitorijumu se nalaze i pomoćni dokumenti, uključujući `README.md` i `docs/crud-coverage.md`, koji dodatno opisuju lokalno pokretanje, trenutne funkcionalnosti i CRUD pokrivenost poslovnih celina.

## 11. Ograničenja i mogućnosti daljeg unapređenja

Trenutna implementacija je pogodna za lokalnu demonstraciju i odbranu projekta. Pojedina ograničenja su namerna, jer doprinose bezbednosti i jasnoći školskog projekta.

Ograničenja su:

- Stripe integracija je namenjena isključivo probnom okruženju i ne obrađuje stvarna plaćanja;
- plaćanje u prodavnici implementirano je za auto-delove, dok se aukcijski pobednici vode kroz prihvaćene prodaje;
- istorijski zapisi o ponudama, porudžbinama, plaćanjima i *webhook* događajima ne menjaju se proizvoljno kroz interfejs;
- trajno brisanje je ograničeno za podatke koji su povezani sa poslovnom istorijom;
- produkciona SMTP i produkciona platna konfiguracija nisu uključene u repozitorijum.

Moguća dalja unapređenja uključuju:

- produkciono postavljanje aplikacije;
- naprednije izveštaje i izvoz podataka za administratora;
- dodatnu analitiku prodaje i aktivnosti aukcija;
- unapređenje pretrage vozila i auto-delova;
- dodatne metode obaveštavanja korisnika, na primer elektronsku poštu ili push obaveštenja;
- detaljniju proveru pristupačnosti korisničkog interfejsa;
- posebnu korisničku dokumentaciju za administratore i demonstratore;
- dodatne integracione testove za rad sa MySQL profilom.

## 12. Literatura i reference

- Spring Boot dokumentacija: https://docs.spring.io/spring-boot/
- Spring Security dokumentacija: https://docs.spring.io/spring-security/reference/
- Spring Data JPA dokumentacija: https://docs.spring.io/spring-data/jpa/reference/
- Hibernate Validator dokumentacija: https://hibernate.org/validator/documentation/
- Flyway dokumentacija: https://documentation.red-gate.com/fd
- Stripe dokumentacija: https://docs.stripe.com/
- H2 Database dokumentacija: https://www.h2database.com/html/main.html
- MySQL dokumentacija: https://dev.mysql.com/doc/

## 13. Zaključak

U okviru projekta razvijen je informacioni sistem Autostrada Auctions, namenjen digitalnom tržištu automobila i auto-delova. Implementacijom su obuhvaćeni javni pregledi kataloga, registracija i autentifikacija korisnika, korisnički profili, postavljanje automobila, administratorska moderacija, aukcijske ponude, test-vožnje, praćenje aukcija, obaveštenja, prodavnica auto-delova, korpa, porudžbine i obrada plaćanja kroz Stripe probno okruženje.

Sistem koristi slojevitu Spring Boot arhitekturu, relacione baze podataka, Flyway migracije, Spring Security autorizaciju i automatizovane testove. Poslovna pravila su implementirana tako da se štite uloge, vlasništvo, rokovi aukcija, validnost unosa, zalihe i finansijski zapisi. Podaci koji imaju poslovni značaj čuvaju se kroz statuse i istoriju, umesto da se proizvoljno brišu.

Na osnovu pregleda repozitorijuma i uspešnog izvršavanja testova može se zaključiti da aplikacija predstavlja funkcionalno i praktično rešenje za demonstraciju tržišta automobila, aukcija i prodavnice auto-delova. Projekat ispunjava proverene uslove za predaju dokumentacije i pripremu odbrane, uz jasne mogućnosti za dalji razvoj i produkciono unapređenje.
