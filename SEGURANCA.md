# Alteracoes de seguranca do projeto TarefasRH

Este documento resume as alteracoes feitas para reduzir o risco de expor senhas, tokens, chaves e links internos ao enviar o projeto para GitHub ou para ferramentas de IA.

## O que foi alterado

### 1. Arquivos sensiveis foram separados do codigo

Antes, o projeto tinha dados sensiveis dentro de arquivos que poderiam ser enviados junto com o codigo, como:

- senha do banco MySQL em `application.properties`;
- chave JSON da service account do Google em `back/src/main/resources/credentials.json`;
- links diretos da planilha e do Looker Studio no template do frontend.

Agora, os valores reais ficam em arquivos locais ignorados pelo Git:

```text
front/.env
back/src/main/resources/application-local.properties
back/secrets/google-credentials.json
```

Esses arquivos devem existir na sua maquina, mas nao devem ir para GitHub nem ser enviados para IA.

### 2. Foi criado um `.gitignore`

O arquivo `.gitignore` impede o versionamento de dependencias, builds e segredos locais:

```text
front/node_modules/
back/target/
front/.env
back/src/main/resources/application-local.properties
back/src/main/resources/credentials.json
back/secrets/
```

Isso evita que o Git inclua arquivos que nao devem ser publicados.

### 3. `application.properties` virou configuracao segura

O arquivo versionavel:

```text
back/src/main/resources/application.properties
```

agora usa variaveis e valores padrao seguros:

```properties
spring.config.import=optional:file:src/main/resources/application-local.properties

spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:}
google.sheets.id=${GOOGLE_SHEETS_ID:}
google.credentials.path=${GOOGLE_CREDENTIALS_PATH:secrets/google-credentials.json}
```

Os valores reais ficam no arquivo local:

```text
back/src/main/resources/application-local.properties
```

### 4. Foi criado um exemplo de configuracao do backend

Arquivo criado:

```text
back/src/main/resources/application.example.properties
```

Ele mostra quais campos precisam ser configurados, mas sem senha real ou chave real.

### 5. A credencial do Google foi movida

Antes:

```text
back/src/main/resources/credentials.json
```

Agora:

```text
back/secrets/google-credentials.json
```

O backend foi ajustado para ler esse arquivo local ou, em ambiente de deploy, a variavel:

```text
GOOGLE_CREDENTIALS_JSON
```

### 6. O Maven foi ajustado para nao empacotar segredos

O `back/pom.xml` foi configurado para nao copiar arquivos sensiveis para `target/classes`:

```xml
<exclude>application-local.properties</exclude>
<exclude>credentials.json</exclude>
<exclude>**/*credentials*.json</exclude>
```

Isso reduz o risco de vazamento ao gerar build ou ZIP.

### 7. Links do Sheets e Looker sairam do template

Antes, os links estavam fixos no arquivo:

```text
front/src/views/dashboard/gestor.ejs
```

Agora, eles vem do arquivo local:

```text
front/.env
```

Variaveis usadas:

```env
GOOGLE_SHEETS_URL=
LOOKER_STUDIO_URL=
```

Se essas variaveis existirem, os botoes aparecem. Se nao existirem, os botoes ficam ocultos.

## Estrutura nova

Estrutura relevante depois das alteracoes:

```text
TarefasRH-Prototipo/
  .gitignore
  SEGURANCA.md
  README.md
  iniciar.bat

  back/
    pom.xml
    secrets/
      google-credentials.json              # local, nao enviar
    src/main/resources/
      application.properties               # pode enviar
      application.example.properties       # pode enviar
      application-local.properties         # local, nao enviar
    src/main/java/
      com/potiguar/tarefasrh/service/
        GoogleSheetsService.java

  front/
    .env                                  # local, nao enviar
    .env.example                          # pode enviar
    package.json
    src/
      app.js
      views/dashboard/gestor.ejs
```

## O que voce deve alterar agora

### 1. Criar uma nova chave da service account do Google

Recomendado: sim, faca isso.

Motivo: a chave antiga ja existiu em um arquivo dentro do projeto. Mesmo que ela tenha sido removida antes de ir ao GitHub, ela pode ter ficado em copias locais, ZIPs, historico de ferramentas, prints ou uploads anteriores.

Passos gerais:

1. Entre no Google Cloud Console.
2. Va em IAM e service accounts.
3. Abra a service account usada no projeto.
4. Revogue/exclua a chave antiga.
5. Crie uma nova chave JSON.
6. Salve o novo arquivo como:

```text
back/secrets/google-credentials.json
```

7. Compartilhe a planilha do Google Sheets com o e-mail da service account nova, se necessario.

### 2. Trocar a senha do banco MySQL

Recomendado: sim, principalmente se essa senha era usada em outros lugares.

No projeto, atualize a senha apenas no arquivo local:

```text
back/src/main/resources/application-local.properties
```

Exemplo:

```properties
spring.datasource.username=root
spring.datasource.password=sua_nova_senha
```

Nao coloque a senha real em:

```text
back/src/main/resources/application.properties
back/src/main/resources/application.example.properties
README.md
```

### 3. Trocar o `SESSION_SECRET` do frontend

Recomendado: sim.

No arquivo local:

```text
front/.env
```

use um valor longo e aleatorio:

```env
SESSION_SECRET=um_valor_longo_aleatorio_e_privado
```

Nao use valores curtos, comuns, sequenciais, nomes do projeto ou qualquer coisa previsivel.

### 4. Revisar usuarios e senhas iniciais do sistema

O projeto ainda tem usuarios iniciais de prototipo no codigo, como senhas padrao para demonstracao.

Arquivos relevantes:

```text
back/src/main/java/com/potiguar/tarefasrh/config/DataInitializer.java
README.md
```

Se o projeto for publicado ou demonstrado fora da sua maquina, troque as senhas padrao ou deixe claro que sao apenas credenciais de demo.

## Checklist antes de enviar para GitHub

Rode:

```powershell
cd C:\Users\PEDRO-PC\Desktop\TarefasRH-Prototipo
git status
```

Confirme que NAO aparecem estes arquivos:

```text
front/.env
back/secrets/google-credentials.json
back/src/main/resources/application-local.properties
front/node_modules/
back/target/
```

Se algum deles aparecer no `git status`, nao faca commit ainda.

## Checklist antes de enviar para IA

Nao envie a pasta inteira em ZIP sem limpar antes.

Nunca envie:

```text
front/.env
back/secrets/
back/src/main/resources/application-local.properties
back/target/
front/node_modules/
```

Para pedir ajuda a uma IA, prefira enviar apenas arquivos de codigo e exemplos sem segredo, como:

```text
application.properties
application.example.properties
.env.example
GoogleSheetsService.java
pom.xml
```

## Como saber se esta funcionando

Backend:

```powershell
cd C:\Users\PEDRO-PC\Desktop\TarefasRH-Prototipo\back
.\mvnw.cmd spring-boot:run
```

Frontend:

```powershell
cd C:\Users\PEDRO-PC\Desktop\TarefasRH-Prototipo\front
npm run dev
```

Ou use:

```powershell
C:\Users\PEDRO-PC\Desktop\TarefasRH-Prototipo\iniciar.bat
```

Se os botoes do Google Sheets e Looker Studio nao aparecerem no dashboard do gestor, confira se o `front/.env` tem:

```env
GOOGLE_SHEETS_URL=...
LOOKER_STUDIO_URL=...
```
