# SIPTV Uploader (proiect Android Studio)

Aplicație simplă care completează automat și trimite formularul de pe
`https://siptv.app/mylist/` (MAC, Username, Parolă, PIN opțional), ca să nu
mai faci asta manual din browser de fiecare dată.

## Funcții: parolă presetată + istoric liste

- **Parola** se completează automat cu `parola` de fiecare dată când deschizi
  aplicația (o poți schimba manual dacă ai nevoie de o parolă diferită la un
  MAC anume).
- La fiecare "Trimite", intrarea (MAC + Username + Parolă + PIN) se salvează
  local pe telefon, într-o listă de **istoric** afișată sub formular. Atingi
  o intrare din istoric și câmpurile se completează automat cu acele date,
  ca să retrimiți rapid aceeași listă sau să pornești de la ea.
- Istoricul ține minte ultimele 30 de liste adăugate și rămâne salvat chiar
  și după ce închizi aplicația (folosește `SharedPreferences`, stocare locală
  pe telefon — nu se trimite nicăieri).

## Cum îl compilezi (durează ~2 minute)

1. Instalează [Android Studio](https://developer.android.com/studio) (gratuit),
   dacă nu îl ai deja.
2. Deschide Android Studio -> **Open** -> selectează folderul `SipTvUploader`
   (acest folder, cel care conține `settings.gradle`).
3. Așteaptă să se termine sincronizarea Gradle (prima dată descarcă niște
   fișiere, poate dura câteva minute).
4. Conectează telefonul Android prin USB (cu Depanare USB activată) sau
   pornește un emulator.
5. Apasă butonul verde **Run ▶** din bara de sus.
6. Aplicația se instalează și pornește pe telefon/emulator.

Dacă vrei direct un fișier `.apk` de instalat manual pe orice telefon:
`Build -> Build Bundle(s) / APK(s) -> Build APK(s)`, apoi fișierul apare în
`app/build/outputs/apk/debug/app-debug.apk`.

## Cum funcționează

Aplicația are un ecran cu 4 câmpuri (MAC, Username, Parolă, PIN opțional) și
un buton "Trimite direct pe siptv.app". Când apeși butonul:

1. Din Username și Parolă, aplicația construiește automat link-ul listei, în
   formatul:
   `http://rezerva.tvabc.eu:80/get.php?username=USER&password=PAROLA&type=m3u_plus&output=ts`
2. Se încarcă pagina reală `siptv.app/mylist/` într-un WebView (vizibil, sub
   formular, ca să vezi ce se întâmplă).
3. După ce pagina s-a încărcat complet, aplicația injectează JavaScript care:
   - completează câmpul MAC,
   - comută formularul pe modul "URL" (link extern, nu fișier încărcat),
   - completează câmpul URL cu link-ul construit la pasul 1,
   - completează PIN-ul, dacă ai introdus unul,
   - apasă automat butonul de trimitere (Send/Upload/Submit).
4. Sub formular apare un mesaj cu rezultatul (`mac`, `urlTab`, `url`, `pin`,
   `submit` — `true` dacă pasul respectiv a reușit, `false` dacă nu).

## Dacă nu funcționează din prima (foarte posibil)

Site-ul își poate schimba structura paginii, iar JavaScript-ul din
`MainActivity.kt` caută câmpurile "după nume" (ex: orice input care conține
cuvântul "mac" în id/name/placeholder). Dacă vezi în mesajul de rezultat că un
câmp nu a fost găsit (`false`):

1. Deschide `https://siptv.app/mylist/` în Chrome pe calculator.
2. Click-dreapta pe câmpul respectiv -> **Inspect**.
3. Trimite-mi (mie, Claude) exact ce vezi acolo (atributele `id`, `name`,
   `placeholder` ale acelui `<input>`), și ajustez lista de cuvinte cheie din
   funcția `findInput(...)` / `clickByText(...)` din `MainActivity.kt`.

## Notă importantă

Această aplicație automatizează completarea unui formular public, exact cum
ai face-o și tu manual din browser — nu ocolește nicio protecție de securitate
(nu există recaptcha pe siptv.app la data la care a fost scris acest cod).
Folosește aplicația doar cu MAC-uri, useraname-uri și parole pentru care ai
dreptul să faci asta (propriile tale liste / clienți care ți-au dat acordul).
