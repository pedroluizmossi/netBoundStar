# Guia de Setup - Fase 9: Geolocalização & Bandeiras 🌍

## O que você precisa fazer manualmente:

### 1. Baixar o Banco de Dados MaxMind (GeoLite2-Country.mmdb)

**Passos:**
1. Visite: https://www.maxmind.com/en/geolite2-free-geolocation-database
2. Faça cadastro (gratuito) ou faça login
3. Download o arquivo **GeoLite2 Country** (arquivo `.mmdb`)
4. Crie a pasta: `netBoundStar-view/src/main/resources/geo/`
5. Coloque o arquivo lá com o nome exato: `GeoLite2-Country.mmdb`

**Resultado:**
```
netBoundStar-view/src/main/resources/geo/GeoLite2-Country.mmdb
```

### 2. Baixar Ícones de Bandeiras

**Passos:**
1. Baixe um pacote de flag icons (recomendação):
   - GitHub: https://github.com/lipis/flag-icons (versão SVG OU PNG)
   - Ou procure por "Flag Icons ISO 3166" no Google
2. Você precisa de ícones nomeados com **2 letras** (maiúsculas ou minúsculas):
   - `br.svg` ou `BR.svg` ou `br.png` ou `BR.png` (Brasil)
   - `us.svg` ou `US.svg` ou `us.png` ou `US.png` (USA)
   - `de.svg` ou `DE.svg` ou `de.png` ou `DE.png` (Germany)
   - `fr.svg` ou `FR.svg` ou `fr.png` ou `FR.png` (France)
   - `jp.svg` ou `JP.svg` ou `jp.png` ou `JP.png` (Japan)
   - ...etc
3. **Formato**: Pode ser **SVG** (recomendado, menor tamanho) ou **PNG**
4. **Tamanho recomendado**: Para PNG: 24x24px ou 32x32px
5. **Case**: Não importa se é maiúscula ou minúscula (o sistema tenta ambas)
6. Crie a pasta: `netBoundStar-view/src/main/resources/flags/`
7. Coloque todos os arquivos lá dentro

**Resultado (exemplo com minúsculas):**
```
netBoundStar-view/src/main/resources/flags/
├── br.svg
├── us.svg
├── de.svg
├── fr.svg
├── jp.svg
├── ru.svg
└── ... (quantos mais, melhor!)
```

**OU com PNG:**
```
netBoundStar-view/src/main/resources/flags/
├── br.png
├── us.png
├── de.png
├── fr.png
├── jp.png
├── ru.png
└── ...
```

**OU misturado:**
```
netBoundStar-view/src/main/resources/flags/
├── br.svg
├── US.png
├── de.svg
├── FR.svg
├── jp.png
└── ...
```

## Estrutura Final Esperada:

```
netBoundStar-view/src/main/resources/
├── geo/
│   └── GeoLite2-Country.mmdb (opcional, mas recomendado)
└── flags/
    ├── BR.svg (ou BR.png)
    ├── US.svg (ou US.png)
    ├── DE.svg (ou DE.png)
    ├── FR.svg (ou FR.png)
    ├── CN.svg (ou CN.png)
    ├── AU.svg (ou AU.png)
    └── ... (vários countries)
```

## Como o Sistema Funciona:

1. **Ao iniciar**, o `GeoService` carrega o arquivo `.mmdb` em memória
2. **Para cada IP remoto**, o sistema:
   - Resolve o hostname via DNS (já existente)
   - Resolve o país via GeoIP (MaxMind)
   - Carrega a bandeira correspondente do cache
3. **No canvas**, em vez de uma bolinha branca, desenha a bandeira
4. **Se não encontrar** a bandeira ou o banco de dados, volta para bolinha branca (fallback)

## Verificação:

Após colocar os arquivos, quando você rodar a aplicação:
- Veja no console se aparece: `✓ GeoLite2 carregado com sucesso!`
- Se aparecer `⚠ AVISO: Arquivo GeoLite2-Country.mmdb não encontrado`, coloque o arquivo na pasta certa
- As bandeiras aparecerão automaticamente conforme os IPs forem resolvidos

## Dicas:

- Você não precisa de TODOS os países - coloque os que quiser
- As bandeiras mais comuns são: us, br, de, fr, gb, jp, ru, cn, au, ca
- **SVG é preferível a PNG** (menor tamanho, melhor qualidade em qualquer resolução)
- **Case não importa**: `br.svg`, `BR.svg`, `Br.svg` - tudo funciona!
- O sistema tenta carregar SVG primeiro, depois PNG
- O sistema tenta maiúscula primeiro, depois minúscula
- Se um país não tiver bandeira, mostra bolinha branca (sem erro)
- O cache da memória evita carregar a mesma imagem várias vezes
- Você pode misturar formatos: `br.svg`, `US.png`, `de.svg` (tudo junto funciona!)

Boa sorte! 🌍🚀

