## Generate keys

```bash
gpg --gen-key
```

## List them

```bash
gpg --list-keys
```

Output is like:

```text
$ gpg --list-secret-keys --keyid-format=long
/home/steven/.gnupg/pubring.kbx
-------------------------------
sec   rsa3072/99CEEF7F4D5085C2 2025-02-22 [SC] [expires: 2027-02-22]
      1620F76B23ACC52D73DDEB8C99CEEF7F4D5085C2
uid                 [ultimate] Steven Van Ingelgem <steven@vaningelgem.be>
ssb   rsa3072/3D2ADE7187466D99 2025-02-22 [E] [expires: 2027-02-22]
```

## Upload them

```bash
gpg --keyserver keyserver.ubuntu.com --send-keys 99CEEF7F4D5085C2
```

## Show the public key

```bash
gpg --armor --export 99CEEF7F4D5085C2
```

Output:

```text
-----BEGIN PGP PUBLIC KEY BLOCK-----
[...]
-----END PGP PUBLIC KEY BLOCK-----
```

## Set the GPG key in your github profile

URL: https://github.com/settings/keys

## Export the private key

```bash
gpg --armor --export-secret-key steven@vaningelgem.be
```

Output:

```text
-----BEGIN PGP PRIVATE KEY BLOCK-----
[...]
-----END PGP PRIVATE KEY BLOCK-----
```
