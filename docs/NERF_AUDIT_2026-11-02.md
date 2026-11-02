# Audyt nerfów i receptur — 2 listopada 2026

## Decyzje i poprawki po audycie

Poniższe ustalenia i liczby opisują stan z commita podanego w zakresie audytu;
nie są opisem bieżących wyników po poprawkach. Decyzje i zmiany z 3 listopada:

| Ustalenie | Stan |
| --- | --- |
| Ruby Juice, Samarium, Endstone, Raw Neutronium, Indium Comb oraz uzyski Ilmenite Slag i Sphalerite w maszynach przemysłowych | Poprawione w `1419ceb`; obie mapy są sprawdzane przez testy receptur. |
| Stare receptury stali i aluminium w Alloy Blast Smelterze | Poprawione w `0c17277`, także dla aluminium z TConstruct. |
| Utrata fizycznego Integrated Circuit przy niszczeniu maszyny | Poprawiona w `0fb72d3`, również dla natywnej ścieżki dropu przy wybuchu. |
| Najwyższa warstwa świata dla Lightning Rod | Poprawiona w `5656f6a`. |
| Tańsze przepisy Circuit Assembly Line | Zamierzone; nie są błędem i pozostały bez zmian. |
| Advanced Seismic Prospector przyjmuje materiały bez certyfikatu | Świadomie pozostawione bez zmian jako mało istotne; sama praca pozostaje blokowana. |
| Zapisany ghost circuit działa do pierwszego otwarcia GUI | Świadomie pozostawione zachowanie migracji istniejących zapisów. |

Poprawka zużycia pary z równoległej pracy została wcześniej zapisana w
`4410124`; nie była częścią pierwotnego audytu. Testy integracyjne po powyższych
zmianach ponownie sprawdziły objęte nimi maszyny i wskazania pary.

## Zakres i sposób weryfikacji

Punkt odniesienia Extra Nerf: commit `7977d2c634aa471a5cd8a1798b6de3f23b8db60a`. Przegląd porównuje dokumentację z kodem moda, GT5-Unofficial **5.09.54.261** (`eee6ffbb00d8f7b404f8e8d6db87462f8d680e95`, z BartWorks, GT++, Good Generator, TecTech i pozostałymi dodatkami scalonymi z GT5) oraz NewHorizonsCoreMod **2.9.99** (`9f108160f88f7d6ba7e20d830eeee760d7198144`). To wersje z `dependencies.gradle`.

Nerf ilości pary i jego prezentacja w NEI/Waila są poza tym audytem: równolegle analizuje je inny agent. `MixinMetaTileEntity` sprawdzono tutaj w części dotyczącej blokady energii; przeliczenie pary pominięto.

Oprócz przeglądu kodu uruchomiono **oddzielną kopię** pełnego serwera opartego na daily 2026-10-30+807, z podmienionymi JAR-ami GT5 i NHCore na powyższe wersje oraz kopią Extra Nerf z wymienionego commita. Jednorazowy obserwator wyłącznie odczytywał końcowe rejestry; poniższe oznaczenie **rejestr** oznacza jego wynik, a nie ręcznie rozegraną recepturę. Telemetria i team tracking były wyłączone. Wbudowany audyt moda przeszedł (`modified=1828, added=52, removed=2160`), mimo wykrytych niżej obejść.

Uruchomiono 156 istniejących testów jednostkowych: 0 błędów i 0 porażek. Istniejący zestaw integracyjny moda uruchomiono na tym serwerze; sprawdzone obszary i granice opisano na końcu. Raport nie dowodzi braku każdego obejścia przez pozostałe, niescalone mody, zadania, loot, nietypowe konfiguracje ani wszystkich stanów świata. Dla obejść rejestrowych potwierdzono końcowe definicje i przypisanie maszyn do map, ale nie uruchamiano ręcznie każdej receptury w grze.

## Ustalenia

### P1. Ruby Juice: cały usunięty łańcuch pozostał w mapach multiblocków

**Dowód: rejestr i kod.** [`AluminiumNerf`](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/AluminiumNerf.java) usuwa osiem receptur z `mixerRecipes` i jedną z `centrifugeRecipes`, lecz nie usuwa ich odpowiedników z map GT++.

W końcowym rejestrze po poprawnym starcie:

- `gt.recipe.mixer`: **0** receptur Ruby Juice; `gt.recipe.multimixer`: **8** starych receptur, wszystkie cztery postacie Ruby i warianty 1/9 sztuk.
- `gt.recipe.centrifuge`: **0** receptur wykorzystujących Ruby Juice; `gt.recipe.multicentrifuge`: **1** stara receptura. Za 1000 mB soku zwraca ona 2 Aluminium Hydroxide i pozostałe produkty oraz 1000 mB HCl.

Oznacza to działającą drogę: Industrial Mixer → Industrial Centrifuge / Spinmatron. GT++ [kopiuje zwykłe mapy](https://github.com/GTNewHorizons/GT5-Unofficial/blob/eee6ffbb00d8f7b404f8e8d6db87462f8d680e95/src/main/java/gtPlusPlus/GTplusplus.java#L145-L155) przed późnymi zmianami Extra Nerf; [generator](https://github.com/GTNewHorizons/GT5-Unofficial/blob/eee6ffbb00d8f7b404f8e8d6db87462f8d680e95/src/main/java/gtPlusPlus/xmod/gregtech/loaders/RecipeGenMultisUsingFluidInsteadOfCells.java#L55) tworzy osobne wpisy.

### P1. Samarium i Endstone: przemysłowe maszyny zachowują pierwotny uzysk

**Dowód: rejestr i kod.** [`ChemicalAndMiscNerf`](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/ChemicalAndMiscNerf.java) zmienia te wpisy tylko w podstawowych mapach.

| Przepis | Zmieniony singleblock | Zachowany multiblock |
| --- | --- | --- |
| 6 Dephosphated Samarium Concentrate | 5% szansy na Samarium | **90%** w `multicentrifuge` — 18 razy wyższa wartość oczekiwana |
| Samarium Chloride-Sodium Chloride Blend, circuit 1 i 9 | 10% szansy na Samarium | **100%** w `multielectro` — 10 razy wyższa wartość oczekiwana |
| 36 Endstone Dust | 10% na 3 Tungstate Dust i 1500 mB helu | **37,5%** i **4320 mB** w `multicentrifuge` |

Przyczyna jest ta sama co dla Ruby Juice. Przykładowo Staballoy jest w tym samym module zmieniany świadomie w **obu** mapach; wbudowany audyt pozostałych trzech zmian nie wymaga tego.

### P1. Raw Neutronium: przemysłowy mixer zużywa dziesięć razy mniej plazmy

**Dowód: rejestr i kod.** [`CoreModRecipeNerf.modifyRawNeutronium`](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/CoreModRecipeNerf.java) zmienia tylko `mixerRecipes`. Końcowy rejestr pokazuje tę samą porcję: jeden Neutronium Dust + circuit 1 → jeden Raw Neutronium Dust, ale:

- zwykły mixer: **1440 mB** Helium Plasma;
- `gt.recipe.multimixer`: **144 mB** Helium Plasma, czyli koszt sprzed nerfa.

### P1. Stare receptury stali pozostają w Alloy Blast Smelterze

**Dowód: rejestr i kod.** [`BlastFurnaceNerf.modifySteelEBF`](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/BlastFurnaceNerf.java) usuwa przepis Iron + Carbon oraz zwiększa koszt Cast Iron w mapie EBF. GT++ [wcześniej generuje](https://github.com/GTNewHorizons/GT5-Unofficial/blob/eee6ffbb00d8f7b404f8e8d6db87462f8d680e95/src/main/java/gtPlusPlus/xmod/gregtech/loaders/RecipeGenBlastSmelterGTNH.java#L110-L190) osobne przepisy na stopiony metal; moduł ich nie poprawia.

Końcowy `gt.recipe.alloyblastsmelter` zawiera:

- 1 Iron Dust + 1 Coal Dust + circuit 11 + 1000 mB Oxygen → **144 mB molten Steel**, 400 ticków przy 120 EU/t;
- 1 Cast Iron Dust + circuit 11 + 1000 mB Oxygen → **144 mB molten Steel**, 80 ticków przy 120 EU/t.

Pierwsza ścieżka odtwarza usunięty wariant, druga omija nowy koszt tlenu 2000 mB na sztabkę. Stopioną stal można dalej zestalić. To osobna maszyna i może wymagać późniejszego etapu progresji; nie oznacza, że sam EBF przyjmuje stare receptury.

### P1. Alloy Blast Smelter zachowuje stare warianty aluminium

**Dowód: rejestr i kod.** Ten sam mechanizm dotyczy [`BlastFurnaceNerf.modifyAluminiumEBF`](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/BlastFurnaceNerf.java): końcowy rejestr EBF zawiera tylko dwa zmienione warianty Aluminium Dust, ale Alloy Blast Smelter nadal ma **dziewięć** wcześniejszych wariantów tej porcji — bez gazu, z azotem i siedmioma usuniętymi z EBF gazami. Wszystkie produkują 144 mB molten Aluminium.

Przykładowe czasy bazowe przed OC wynoszą **60 s bez gazu** i **48 s z 1000 mB azotu** w Alloy Blast Smelterze, wobec odpowiednio **140 s** i **90 s** w obecnych przepisach EBF. Wariant z 100 mB Oganessonu nadal zajmuje **14,4 s** w Smelterze, mimo usunięcia jego odpowiednika z EBF. Dodatkowy wynik wyszukiwania z Aluminium Dust dotyczył osobnej receptury strontu; nie wliczam go do tych dziewięciu wariantów.

### P2. Przemysłowa wirówka omija także redukcję Indium z plastra

**Dowód: rejestr i kod.** [`modifyPraseodymiumCentrifuge`](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/CoreModRecipeNerf.java) zmienia `centrifugeRecipes`, ale nie jej przemysłową kopię. Dla 8 Indium Comb i 1000 mB zużytego paliwa torowego wynik Indium to **8 dust z 50% szansą** w zwykłej wirówce oraz nadal **12 dust z 50% szansą** w przemysłowej.

### Decyzja projektowa. Circuit Assembly Line produkuje obwody poza zmienionymi recepturami

Tańsza Circuit Assembly Line jest zamierzona, więc nie wymaga naprawy.

**Dowód: rejestr dla Mainframe, Nano i Quantum; kod dla Good Circuit.** Nerfy w [`CoreModRecipeNerf`](../src/main/java/pl/kuba6000/gtnhextranerf/loader/recipes/CoreModRecipeNerf.java) i [`MixinCircuitAssemblerRecipes`](../src/main/java/pl/kuba6000/gtnhextranerf/mixins/late/MixinCircuitAssemblerRecipes.java) działają na `circuitAssemblerRecipes`; nie zmieniają oddzielnej mapy BartWorks `bw.recipe.cal`.

- **Elite / Mainframe:** w rejestrze są dwa przepisy Circuit Assembly Line na **16** Mainframe z **288 mB Soldering Alloy**, bez Soul Infused Medium. Przepisy w Circuit Assemblerze wymagają 100 mB tego medium na sztukę.
- **Nano i Quantum:** w rejestrze są po trzy receptury każdego procesora na 16 sztuk, w tym stare standardowe i ASMD. Dla standardowych składników jedno opakowanie zawiera 16 komponentów, więc np. 16 Nano wymaga jednego opakowania płytek i jednego opakowania Nano CPU: po **1 na procesor**, w porównaniu z **6 na procesor** w zmienionym Circuit Assemblerze. Podobnie dla Quantum.
- **Integrated Logic Circuit:** [kod NHCore](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/9f108160f88f7d6ba7e20d830eeee760d7198144/src/main/java/com/dreammaster/gthandler/recipes/CircuitAssemblyLineRecipes.java#L30-L43) przewiduje 16 sztuk z jednego opakowania 16 ILC Chipów, tj. **1 chip na sztukę**, wobec 4 w zmienionym Circuit Assemblerze.
- **Good Circuit:** [kod NHCore](https://github.com/GTNewHorizons/NewHorizonsCoreMod/blob/9f108160f88f7d6ba7e20d830eeee760d7198144/src/main/java/com/dreammaster/gthandler/recipes/CircuitAssemblyLineRecipes.java#L232-L242) rejestruje 16 sztuk z 32 Basic Electronic Circuits, tj. **2 na sztukę**, podczas gdy zmieniony Circuit Assembler wymaga 4.

To celowo tańsza ścieżka **po uzyskaniu odpowiedniej linii i imprintu**; zmodyfikowane receptury Circuit Assemblera zachowują swoje wymagania. Dokumentacja ogranicza nerf Nano/Quantum do czterech przepisów Circuit Assemblera, a SoC pozostawia osobno. Użytkownik potwierdził po audycie, że receptur Circuit Assembly Line nie należy zmieniać.

### P2. Fizyczny Integrated Circuit przepada przy rozebraniu maszyny

**Dowód: analiza kodu; brak osobnego testu rozbiórki.** [`MixinConfigurationCircuitSupport`](../src/main/java/pl/kuba6000/gtnhextranerf/mixins/late/MixinConfigurationCircuitSupport.java) umieszcza rzeczywisty przedmiot w dawnym slocie widma, ale GT nadal uznaje ten indeks za nieważny w [`MTEBasicMachine.isValidSlot`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/eee6ffbb00d8f7b404f8e8d6db87462f8d680e95/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L325-L329) i [`MTEHatchInputBus.isValidSlot`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/eee6ffbb00d8f7b404f8e8d6db87462f8d680e95/src/main/java/gregtech/api/metatileentity/implementations/MTEHatchInputBus.java#L121-L124). [`BlockMachines.breakBlock`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/eee6ffbb00d8f7b404f8e8d6db87462f8d680e95/src/main/java/gregtech/common/blocks/BlockMachines.java#L473-L505) wyrzuca tylko ważne sloty; zwykła maszyna nie zapisuje tego przedmiotu w NBT zwracanego bloku.

Scenariusz: włożyć prawdziwy Programmed Circuit do maszyny LV/MV lub odpowiedniego input busa i rozebrać blok bez uprzedniego wyjęcia układu. Układ nie jest objęty normalnym dropem. Ta sama przesłanka występuje przy wybuchu. Zalecana reprodukcja: rzeczywista maszyna, umieszczenie przez GUI i zebranie dropów po rozbiórce, także z NBT przedmiotu.

### P3. Zablokowany Advanced Seismic Prospector przyjmuje materiały

**Dowód: analiza kodu; brak osobnego testu interakcji.** [`MixinProgressionTickGate`](../src/main/java/pl/kuba6000/gtnhextranerf/mixins/late/MixinProgressionTickGate.java) zatrzymuje `onPostTick`, ale nie interakcje gracza. Native GT [w `onRightclick`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/eee6ffbb00d8f7b404f8e8d6db87462f8d680e95/src/main/java/gregtech/common/tileentities/machines/basic/MTEAdvSeismicProspector.java#L111-L121) pobiera np. 16 TNT i ustawia rozpoczęte zadanie bez sprawdzenia certyfikatu. Dotyczy zarejestrowanych wariantów LV–EV. Sama praca nadal jest zatrzymana przez gate; chodzi o przyjęcie materiału, nie o dowiedzioną produkcję bez certyfikatu.

### P2. Zapisany ghost circuit działa do pierwszego otwarcia GUI

**Dowód: analiza kodu; zachowanie jest opisane w dokumentacji.** Mixin czyści stack o rozmiarze 0 dopiero podczas tworzenia slotu GUI. Wcześniej [`MTEBasicMachine.getAllInputs`](https://github.com/GTNewHorizons/GT5-Unofficial/blob/eee6ffbb00d8f7b404f8e8d6db87462f8d680e95/src/main/java/gregtech/api/metatileentity/implementations/MTEBasicMachine.java#L798-L803) nadal przekazuje go do wyszukiwania receptur GT jako niewyczerpywalny circuit. Istniejąca zautomatyzowana maszyna może więc po aktualizacji pracować bez fizycznego układu, dopóki ktoś nie otworzy GUI. To świadomie udokumentowana granica migracji, ale z punktu widzenia rozgrywki także obejście wymagania posiadania układu. Nie sprawdzono przez osobny scenariusz odczytu starego zapisu.

### P3. Dwie zmiany uzysku nie docierają do maszyn przemysłowych

**Dowód: rejestr i kod.** To nie są tańsze obejścia nerfa, tylko niespójne wyniki:

- Ilmenite Slag: zwykła wirówka ma dodane 10% na Gallium i czas 10 s; przemysłowa zachowała **brak Gallium i czas 2 s**.
- Sphalerite: zwykły Electrolyzer ma 10% na Gallium; Industrial Electrolyzer zachował **9,17%**.

### P3. Lightning Rod nie sprawdza najwyższej warstwy świata

**Dowód: analiza kodu.** [`MixinLightningRod`](../src/main/java/pl/kuba6000/gtnhextranerf/mixins/late/MixinLightningRod.java) iteruje z warunkiem `height < world.getHeight() - 1`. Dla świata o wysokości 256 nie sprawdza więc istniejącej warstwy Y=255. Pełny blok nad masztem na Y=255 nie przerwie generacji, mimo dokumentowanego wymogu niezasłoniętego nieba; płot na tej wysokości nie zostanie też policzony do masztu. Istniejący test przeszkody sprawdza Y=161, nie tę granicę. Zachowano tu także granicę pętli upstream; nie przeprowadzono osobnego testu w grze.

## Pokrycie mixinów

Zinwentaryzowano 50 klas mixinów z badanego commita. „Bez nowego ustalenia” znaczy, że podczas tego przeglądu nie wykazano kolejnego konkretnego błędu; **nie** oznacza dowodu poprawności we wszystkich wariantach gry.

| Obszar i klasy | Wynik i granice |
| --- | --- |
| `MixinCraftingManagerAccessor`, `MixinRecipeAccessor`, `MixinShapedOreRecipeAccessor`, `MixinFMLClientHandler` | Mostki craftingu i ekran błędu startowego; przegląd statyczny, ekran klienta nieweryfikowany w tym uruchomieniu. |
| `MixinSmallOres`, `MixinLoaderGTBlockFluid`, `MixinMaterialsInit` | Selekcja małej rudy, komórek i materiałów odpowiada zadeklarowanym punktom. Nie wykonano generacji świata ani cyklu reaktora. |
| `MixinMTESteamTurbine`, `MixinMTEDieselGenerator`, `MixinMTEGasTurbine`, `MixinMTEPlasmaGenerator`, `MixinMTESemiFluidGenerator` | Prześledzono wzór i sposób, w jaki klasy GT używają wydajności. Dla trzech zarejestrowanych turbin parowych dzielenie daje dokładnie 20/30/40%. Brak pełnego pomiaru każdej klasy generatora na świecie. |
| `MixinMTELargeChemicalReactor` | Ustawia 3/4 OC na natywnym `ProcessingLogic`; nie wykonano osobnego pomiaru receptury i renderu tooltipa. |
| `MixinConfigurationCircuitSupport` | Stwierdzono utratę przedmiotu i ograniczenie migracji wyżej. Test jednostkowy samego handlera nie obejmuje rozbiórki. |
| `MixinProcessingShaping`, `MixinCraftingMaterialLoader`, `MixinProcessingWire`, `MixinCircuitAssemblerRecipes`, `MixinMetaGeneratedItem01`, `MixinAssemblyLineRecipes`, `MixinAe2Crafting` | Przejrzano cele i selektory rejestracji. Wbudowane testy integracyjne sprawdziły m.in. transformator ring i rodziny PPS; zamierzone tańsze receptury Circuit Assembly Line opisano wyżej. Test helpera nie jest dowodem każdej rejestracji w każdym wariancie modów. |
| `MixinAssemblyLineDimension`, `MixinLegacyFusionCoils`, `MixinPlasmaForgeCoils` | Testy serwerowe sprawdziły wybrane struktury i warunki; bez nowego ustalenia. |
| `MixinVoidMinerFluids`, `MixinLightningRod`, `MixinCoolantTower`, `MixinMassFabricatorAmplifier`, `MixinEvergladesMachineBan` | Istniejące testy serwerowe przeszły dla sprawdzanych przypadków. Dla Lightning Rod znaleziono niezbadany wcześniej brzeg Y=255. |
| `MixinOzonationSafety`, `MixinOzonationExplosion` | Przejrzano kolejność sprawdzenia receptury, próg i eksplozję; testy granicy 1 000 000/1 000 001 mB przeszły. |
| `MixinEnergyPorts`, `MixinMetaTileEntity` (energia), `MixinWirelessHatchCertification`, `MixinProgressionTickGate`, `MixinProgressionMachineStatus`, `MixinProgressionMachineGui` | Przejrzano bramkę ticku i ścieżki bufora; testy certyfikacji hatchy, transformatorów oraz progresji przeszły w swoich scenariuszach. Nie zweryfikowano wszystkich możliwych interakcji obcych modów z energią. |
| `MixinElectricalCableFire`, `MixinElectricalMachineExplosion`, `MixinSteamPipeDamage`, `MixinSteamExhaustDamage` | Przejrzano punkty obserwacji incydentów; istniejące testy progresji/parowego bezpieczeństwa przeszły dla sprawdzanych przypadków. To nie jest analiza mnożnika zużycia pary. |
| `MixinMeP2PChannelCapacity`, `MixinWirelessConnectionPower` | Sprawdzono modyfikację flag z kodem AE2 oraz wzór kosztu; test P2P przeszedł. Nie wykonano klientowego pomiaru zużycia dla każdej konfiguracji hubów. |
| `MixinItemLootBag`, `MixinLootBagCommand`, `MixinLootGroupsHandler` | Przejrzano punkty otwierania, limitu, reloadu i zapisu; brak nowego ustalenia. Nie dowodzi to poprawności dowolnych własnych tabel XML. |
| `MixinHealingAxeHunger`, `MixinGodforge`, `MixinGTOreDictUnificator` | Przejrzano zakres hooków; FoTG i materiały objęte uruchomionymi kontrolami. Nie wykonywano osobnej sesji klienta z Healing Axe ani testu współbieżności wszystkich innych modów. |
| `MixinSteamOverclockDescriber` oraz parowa część `MixinMetaTileEntity` | Wyłączone z audytu zgodnie z zakresem. |

## Pokrycie modułów receptur

Przejrzano rejestrację i selektory wszystkich modułów z `RecipeLoader`: `CraftingNerf`, `AluminiumNerf`, `BlastFurnaceNerf`, `ChemicalAndMiscNerf`, `CoreModRecipeNerf`, `SlimeRecipeNerf`, `Ae2RecipeNerf`, `IntergalacticRecipeNerf`, `ProgressionRecipeNerf`, `TungstenSteelProduction`, `Brainrotter`, a także osobne `Ae2AssemblyLineRecipes` i receptury zmieniane przy rejestracji przez mixiny. Wyniki pozytywne wbudowanego audytu oznaczają dopasowanie zadeklarowanych selektorów i liczników, nie wyczerpujące poszukiwanie wszystkich innych producentów danego wyniku.

Przemysłowe kopie przeanalizowano szczególnie dla map Mixer/Centrifuge/Electrolyzer; potwierdzone rozbieżności podano wyżej. Osobno potwierdzono, że wybrane zmiany, m.in. Staballoy, TPV, mieszanina Tungstensteel oraz elektroliza soli Naquadah, obejmują obie mapy w kodzie. W przypadku pozostałych modułów nie należy traktować braku wymienionego błędu jako gwarancji zamknięcia wszystkich wieloetapowych łańcuchów i nietypowych źródeł przedmiotów.

## Wykonana weryfikacja

- Oddzielna kopia źródeł, `compileJava`, `reobfJar`, `test`: **156 testów JUnit, 0 błędów i porażek**. Budowa obejmowała jednorazowy obserwator rejestrów, którego nie dodano do projektu.
- Oddzielna kopia pełnego serwera, GT5 5.09.54.261 i NHCore 2.9.99: uruchomienie do `Done`, końcowy odczyt rejestrów jak wyżej; istniejący testowy artefakt `-PmixinIntegrationTests` zgłosił przejście testów materiałów, mixinów, hatchy, Spatial Assembly Line, Lightning Rod, Void Miner, P2P, casingów, transformatorów, fusion, Plasma Forge, Coolant Tower, Mass Fabricatora, ozonowania, Brainrottera, progresji, egzaminu, bezpieczeństwa pary i Everglades. Serwer zakończono komendą `stop` z kodem 0.
- Porównano też wskazane pliki źródłowe generatorów map GT++ i mechanizmu dropu GT między GT5 **5.09.54.260** i **5.09.54.261**, a pliki Circuit Assembly Line i Mixer w NHCore **2.9.98** i **2.9.99**: w tych plikach nie było różnic. Nie jest to test całej paczki na starszych wersjach.
- Tożsamość i wybrane wyniki ze starego uruchomienia były też kontrolowane analitycznie w źródłach. Nie uruchamiano klienta, nie porównywano Waila/NEI i nie wykonywano osobnych testów każdego przepisu czy wszystkich dodatków spoza scalonego GT5/NHCore.
- Pełne logi izolowanych odczytów są dostępne tymczasowo: `/tmp/gtnh-nerf-audit-SMJMiS/server/audit-server-integration.log` oraz `/tmp/gtnh-nerf-audit-SMJMiS/server/audit-server-final.log`. Wyniki kluczowych odczytów zapisano w raporcie powyżej.

## Pierwotnie zalecana kolejność napraw

1. Objąć właściwe końcowe mapy `mixerNonCellRecipes`, `centrifugeNonCellRecipes` i `electrolyzerNonCellRecipes` tymi samymi zmianami, z kontrolą liczby i charakterystyk wszystkich wariantów po publikacji. Zabezpieczyć Alloy Blast Smelter przed starymi recepturami EBF.
2. Uzgodnić zamierzony koszt na Circuit Assembly Line i odpowiednio zmienić lub jawnie zachować jej receptury; uwzględnić imprint i duże partie, zamiast bezrefleksyjnie mnożyć pojedyncze stacki ponad limit.
3. Zapewnić zachowanie rzeczywistego układu przy demontażu oraz zdecydować, kiedy usuwać stare ghost circuits. Sprawdzić to na działającej maszynie, zapisie świata i dropie przedmiotów.
4. Sprawdzić górną warstwę Lightning Rod i pozostałe opisane różnice. Dla każdej zmiany weryfikować rzeczywisty lookup odpowiedniej maszyny, a nie tylko liczbę operacji audytu.
