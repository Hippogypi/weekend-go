<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue';
import type { Place } from '../services';

const props = defineProps<{
  places: Place[];
}>();

const emit = defineEmits<{
  (e: 'open-detail', placeId: number | string): void;
}>();

const mapContainer = ref<HTMLDivElement | null>(null);
let map: AMap.Map | null = null;
let markers: AMap.Marker[] = [];
let infoWindow: AMap.InfoWindow | null = null;

function waitForAMap(maxWait = 10000): Promise<void> {
  return new Promise((resolve, reject) => {
    const start = Date.now();
    const check = () => {
      if (window.AMap) {
        resolve();
        return;
      }
      if (Date.now() - start > maxWait) {
        reject(new Error('AMap failed to initialize within timeout'));
        return;
      }
      setTimeout(check, 100);
    };
    check();
  });
}

function loadAMapScript(): Promise<void> {
  if (window.AMap) return Promise.resolve();

  const key = import.meta.env.VITE_AMAP_JS_API_KEY;
  if (!key) {
    console.warn('VITE_AMAP_JS_API_KEY not configured');
    return Promise.reject(new Error('Amap key not configured'));
  }

  const securityCode = import.meta.env.VITE_AMAP_SECURITY_JS_CODE;
  if (securityCode && !window._AMapSecurityConfig) {
    window._AMapSecurityConfig = { securityJsCode: securityCode };
  }

  return new Promise((resolve, reject) => {
    const script = document.createElement('script');
    script.type = 'text/javascript';
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${key}`;
    script.onload = () => {
      waitForAMap().then(resolve).catch(reject);
    };
    script.onerror = () => reject(new Error('Failed to load AMap script'));
    document.head.appendChild(script);
  });
}

function initMap(): void {
  if (!mapContainer.value || !window.AMap) return;

  const defaultLng = 116.481488;
  const defaultLat = 39.990464;

  map = new window.AMap.Map(mapContainer.value, {
    zoom: 12,
    center: new window.AMap.LngLat(defaultLng, defaultLat)
  });

  infoWindow = new window.AMap.InfoWindow({
    offset: new window.AMap.Pixel(0, -30)
  });
}

function clearMarkers(): void {
  if (!map) return;
  markers.forEach(m => map!.remove(m));
  markers = [];
  if (infoWindow) {
    infoWindow.close();
  }
}

function addMarkers(places: Place[]): void {
  if (!map || !window.AMap) return;

  clearMarkers();

  if (places.length === 0) return;

  const validPlaces = places.filter(p => {
    const lng = typeof p.longitude === 'string' ? parseFloat(p.longitude) : p.longitude;
    const lat = typeof p.latitude === 'string' ? parseFloat(p.latitude) : p.latitude;
    return lng != null && !isNaN(lng) && lat != null && !isNaN(lat);
  });

  if (validPlaces.length === 0) return;

  validPlaces.forEach(place => {
    const lng = typeof place.longitude === 'string' ? parseFloat(place.longitude) : place.longitude!;
    const lat = typeof place.latitude === 'string' ? parseFloat(place.latitude) : place.latitude!;

    const marker = new window.AMap.Marker({
      position: new window.AMap.LngLat(lng, lat),
      title: place.name
    });

    marker.on('click', () => {
      if (infoWindow) {
        const content = document.createElement('div');
        content.style.padding = '4px 8px';
        content.innerHTML = `<strong style="display:block;margin-bottom:4px;color:#101828;">${place.name}</strong>`;
        const link = document.createElement('a');
        link.href = 'javascript:void(0)';
        link.textContent = '查看详情';
        link.style.color = '#0f766e';
        link.style.fontSize = '13px';
        link.style.textDecoration = 'underline';
        link.addEventListener('click', (e) => {
          e.preventDefault();
          emit('open-detail', place.id);
        });
        content.appendChild(link);
        (infoWindow as any).setContent(content);
        infoWindow.open(map!, marker.getPosition()!);
      }
    });

    map!.add(marker);
    markers.push(marker);
  });

  if (validPlaces.length > 1) {
    const bounds = new window.AMap.Bounds(
      markers[0].getPosition()!,
      markers[0].getPosition()!
    );
    markers.forEach(m => bounds.extend(m.getPosition()!));
    map!.setBounds(bounds, [40, 40, 40, 40]);
  } else {
    const lng = typeof validPlaces[0].longitude === 'string' ? parseFloat(validPlaces[0].longitude) : validPlaces[0].longitude!;
    const lat = typeof validPlaces[0].latitude === 'string' ? parseFloat(validPlaces[0].latitude) : validPlaces[0].latitude!;
    map!.setCenter(new window.AMap.LngLat(lng, lat));
    map!.setZoom(14);
  }
}

let initialized = false;

watch(() => props.places, (newPlaces) => {
  if (!initialized) {
    loadAMapScript()
      .then(() => {
        initMap();
        addMarkers(newPlaces);
        initialized = true;
      })
      .catch(err => console.error('Map init failed:', err));
  } else {
    addMarkers(newPlaces);
  }
}, { deep: true, immediate: true });

onUnmounted(() => {
  clearMarkers();
  if (map) {
    map.destroy();
    map = null;
  }
});
</script>

<template>
  <div ref="mapContainer" class="map-container"></div>
</template>

<style scoped>
.map-container {
  width: 100%;
  height: 360px;
  border: 1px solid #d9dee7;
  border-radius: 8px;
  background: #eef2f6;
}

@media (max-width: 780px) {
  .map-container {
    height: 280px;
  }
}
</style>
