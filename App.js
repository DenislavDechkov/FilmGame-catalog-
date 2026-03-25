/* =============================================
   app.js – Логика на фронтенда
   Комуникира с Java REST API на порт 8080
   ============================================= */

// ---- Примерни данни (fallback ако API не работи) ----
const DEMO_DATA = [
  { id:1, type:'movie', title:'Dune: Part Two',     year:2024, genre:'Sci-Fi',     rating:8.8, emoji:'🏜️', desc:'Пол Атрейдес обединява силите си с фременките на Аракис срещу Харконените.', director:'Denis Villeneuve', duration:166 },
  { id:2, type:'movie', title:'Oppenheimer',         year:2023, genre:'Драма',      rating:8.9, emoji:'☢️', desc:'Биографичен трилър за създателя на атомната бомба Дж. Р. Опенхаймер.', director:'Christopher Nolan', duration:180 },
  { id:3, type:'movie', title:'Deadpool & Wolverine', year:2024, genre:'Екшън',    rating:7.8, emoji:'⚔️', desc:'Дедпул и Върколакът се сдружават за мисия, застрашаваща мултивселената.', director:'Shawn Levy', duration:128 },
  { id:4, type:'movie', title:'Interstellar',        year:2014, genre:'Sci-Fi',     rating:8.7, emoji:'🌌', desc:'Екип астронавти пътува през червеева дупка в търсене на нов дом.', director:'Christopher Nolan', duration:169 },
  { id:5, type:'movie', title:'Joker',               year:2019, genre:'Драма',      rating:8.4, emoji:'🃏', desc:'Артур Флек слиза в спирала на насилие и хаос, превръщайки се в Жокера.', director:'Todd Phillips', duration:122 },
  { id:6, type:'movie', title:'Alien: Romulus',      year:2024, genre:'Трилър',     rating:7.3, emoji:'👽', desc:'Млади колонисти намират изоставена станция и se сблъскват с извънземна заплаха.', director:'Fede Álvarez', duration:119 },
  { id:7, type:'game',  title:'Elden Ring',          year:2022, genre:'RPG',        rating:9.4, emoji:'⚔️', desc:'Отворен свят RPG с мрачна атмосфера. Изследвай Земите Между в търсене на Пръстена.', developer:'FromSoftware', platform:'PC / PS5 / Xbox' },
  { id:8, type:'game',  title:"Baldur's Gate 3",     year:2023, genre:'RPG',        rating:9.6, emoji:'🧙', desc:'Тактически RPG базиран на D&D. Пълна свобода на избор в огромен свят.', developer:'Larian Studios', platform:'PC / PS5' },
  { id:9, type:'game',  title:'Cyberpunk 2077',      year:2020, genre:'RPG',        rating:8.2, emoji:'🌆', desc:'Отворен свят в дистопичния Найт Сити – хакери, корпорации, оцеляване.', developer:'CD Projekt Red', platform:'PC / PS5 / Xbox' },
  { id:10,type:'game',  title:'Hollow Knight',       year:2017, genre:'Приключение',rating:9.1, emoji:'🦋', desc:'Метроидвания в тъмното подземно кралство Халоунест.', developer:'Team Cherry', platform:'PC / Switch / PS4' },
  { id:11,type:'game',  title:'Hades',               year:2020, genre:'Екшън',      rating:9.5, emoji:'🔱', desc:'Roguelite в митологична Гърция – бягай от подземния свят, бори се с богове.', developer:'Supergiant Games', platform:'PC / Switch / PS5' },
  { id:12,type:'game',  title:'Civilization VI',     year:2016, genre:'Стратегия',  rating:8.8, emoji:'🏛️', desc:'Изгради цивилизация от каменната ера до космическата. Дипломация, война и наука.', developer:'Firaxis Games', platform:'PC / Mobile' },
];

// ---- Глобален масив с данни ----
let allItems = [];
let currentTab = 'all';

// ---- Зареждане на данни ----
async function loadData() {
  try {
    // Опит за свързване с Java API
    const res = await fetch('http://localhost:8080/api/catalog', {
      headers: { 'Accept': 'application/json' }
    });
    if (!res.ok) throw new Error('API грешка');
    allItems = await res.json();
    console.log('✅ Данните заредени от Java API');
  } catch (err) {
    // Ако API не е стартиран – ползваме demo данни
    console.warn('⚠️ Java API недостъпен, използвам demo данни:', err.message);
    allItems = [...DEMO_DATA];
  }
  renderAll();
}

// ---- Помощна функция: звезди ----
function getStars(rating) {
  const full  = Math.round(rating / 2);
  const empty = 5 - full;
  return '★'.repeat(full) + '☆'.repeat(empty);
}

// ---- Рендиране на карта ----
function renderCard(item) {
  const isMovie = item.type === 'movie';
  const typeLabel = isMovie ? '🎬 Филм' : '🎮 Игра';
  const badgeClass = isMovie ? 'badge-movie' : 'badge-game';
  const stripeClass = isMovie ? 'movie' : 'game';

  return `
    <div class="card" onclick="openModal(${item.id})">
      <div class="card-stripe ${stripeClass}"></div>
      <div class="card-body">
        <div class="card-header">
          <div class="card-title">${escHtml(item.title)}</div>
          <span class="card-type-badge ${badgeClass}">${typeLabel}</span>
        </div>
        <div class="card-meta">
          <span>${item.year}</span>
          <span class="card-genre">${escHtml(item.genre)}</span>
        </div>
        <div class="card-desc">${escHtml(item.desc || 'Няма описание.')}</div>
        <div class="card-footer">
          <div class="card-rating">
            <span class="stars">${getStars(item.rating)}</span>
            <span class="rating-num">${item.rating}</span>
            <span class="rating-max">/10</span>
          </div>
          <button class="btn-details" onclick="event.stopPropagation(); openModal(${item.id})">Детайли</button>
        </div>
      </div>
    </div>
  `;
}

// ---- Рендиране на решетка ----
function renderGrid(containerId, items) {
  const el = document.getElementById(containerId);
  if (!el) return;
  if (items.length === 0) {
    el.innerHTML = `<div class="no-results"><span class="emoji">🔍</span>Няма намерени резултати.</div>`;
  } else {
    el.innerHTML = items.map(renderCard).join('');
  }
}

// ---- Филтриране ----
function getFiltered() {
  const query  = document.getElementById('searchInput').value.toLowerCase().trim();
  const genre  = document.getElementById('genreFilter').value;
  const minRat = parseFloat(document.getElementById('ratingFilter').value) || 0;

  return allItems.filter(item => {
    const matchSearch = !query || item.title.toLowerCase().includes(query);
    const matchGenre  = !genre  || item.genre === genre;
    const matchRating = item.rating >= minRat;
    return matchSearch && matchGenre && matchRating;
  });
}

function filterItems() {
  const filtered = getFiltered();
  document.getElementById('resultCount').textContent = filtered.length;
  renderGrid('grid-all',    filtered);
  renderGrid('grid-movies', filtered.filter(i => i.type === 'movie'));
  renderGrid('grid-games',  filtered.filter(i => i.type === 'game'));
}

function resetFilters() {
  document.getElementById('searchInput').value = '';
  document.getElementById('genreFilter').value = '';
  document.getElementById('ratingFilter').value = '0';
  filterItems();
}

// ---- Всички таблове ----
function renderAll() {
  filterItems();
}

// ---- Таб навигация ----
function showTab(tab) {
  currentTab = tab;
  ['all','movies','games','add'].forEach(t => {
    document.getElementById(`tab-${t}`).classList.toggle('hidden', t !== tab);
  });
  document.querySelectorAll('.nav-link').forEach((link, idx) => {
    const tabs = ['all','movies','games','add'];
    link.classList.toggle('active', tabs[idx] === tab);
  });
  if (tab !== 'add') filterItems();
}

// ---- Отваряне на модал ----
function openModal(id) {
  const item = allItems.find(i => i.id === id);
  if (!item) return;

  const isMovie = item.type === 'movie';
  const barColor = isMovie ? 'var(--movie-clr)' : 'var(--game-clr)';

  let extraDetails = '';
  if (isMovie) {
    extraDetails = `
      <div class="modal-detail"><strong>Режисьор</strong>${escHtml(item.director || '—')}</div>
      <div class="modal-detail"><strong>Продължителност</strong>${item.duration ? item.duration + ' мин' : '—'}</div>
    `;
  } else {
    extraDetails = `
      <div class="modal-detail"><strong>Разработчик</strong>${escHtml(item.developer || '—')}</div>
      <div class="modal-detail"><strong>Платформи</strong>${escHtml(item.platform || '—')}</div>
    `;
  }

  document.getElementById('modal-content').innerHTML = `
    <div class="modal-type-bar" style="background:${barColor}"></div>
    <div class="modal-emoji">${item.emoji || (isMovie ? '🎬' : '🎮')}</div>
    <h2 class="modal-title">${escHtml(item.title)}</h2>
    <div class="modal-badges">
      <span class="modal-badge">${isMovie ? '🎬 Филм' : '🎮 Игра'}</span>
      <span class="modal-badge">${item.year}</span>
      <span class="modal-badge">${escHtml(item.genre)}</span>
    </div>
    <div class="modal-rating-big">
      <div class="stars">${getStars(item.rating)}</div>
      <span class="num">${item.rating}</span><span style="color:var(--muted);font-family:Arial,sans-serif"> /10</span>
    </div>
    <div class="modal-desc">${escHtml(item.desc || 'Няма описание.')}</div>
    <div class="modal-details">${extraDetails}</div>
  `;

  document.getElementById('modal-bg').classList.remove('hidden');
}

function closeModal(e) {
  if (e.target === document.getElementById('modal-bg')) closeModalBtn();
}

function closeModalBtn() {
  document.getElementById('modal-bg').classList.add('hidden');
}

// ---- Добавяне на запис ----
document.getElementById('newType').addEventListener('change', function() {
  document.getElementById('movie-fields').classList.toggle('hidden', this.value !== 'movie');
  document.getElementById('game-fields').classList.toggle('hidden',  this.value !== 'game');
});

async function addItem(e) {
  e.preventDefault();
  const type = document.getElementById('newType').value;
  const item = {
    id:      Date.now(),
    type,
    title:   document.getElementById('newTitle').value.trim(),
    year:    parseInt(document.getElementById('newYear').value),
    genre:   document.getElementById('newGenre').value,
    rating:  parseFloat(document.getElementById('newRating').value),
    desc:    document.getElementById('newDesc').value.trim(),
    emoji:   type === 'movie' ? '🎬' : '🎮',
  };

  if (type === 'movie') {
    item.director = document.getElementById('newDirector').value.trim();
    item.duration = parseInt(document.getElementById('newDuration').value) || null;
  } else {
    item.developer = document.getElementById('newDeveloper').value.trim();
    item.platform  = document.getElementById('newPlatform').value.trim();
  }

  // Опит за изпращане към Java API
  try {
    const res = await fetch('http://localhost:8080/api/catalog', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(item)
    });
    if (!res.ok) throw new Error();
    const saved = await res.json();
    allItems.push(saved);
  } catch {
    // Ако API е недостъпен – добавяме локално
    allItems.push(item);
  }

  e.target.reset();
  document.getElementById('movie-fields').classList.add('hidden');
  document.getElementById('game-fields').classList.add('hidden');
  showTab('all');
  alert(`✅ "${item.title}" беше добавен успешно!`);
}

// ---- Помощна ескейп функция (за XSS защита) ----
function escHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g,'&amp;')
    .replace(/</g,'&lt;')
    .replace(/>/g,'&gt;')
    .replace(/"/g,'&quot;');
}

// ---- Стартиране ----
loadData();