/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string;
  readonly VITE_AMAP_JS_API_KEY?: string;
  readonly VITE_AMAP_SECURITY_JS_CODE?: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

interface Window {
  AMap?: typeof AMap;
  _AMapSecurityConfig?: { securityJsCode: string };
}

declare namespace AMap {
  class Map {
    constructor(container: string | HTMLElement, opts?: Record<string, unknown>);
    add(overlay: Marker): void;
    remove(overlay: Marker): void;
    setCenter(center: LngLat): void;
    setZoom(zoom: number): void;
    setBounds(bounds: Bounds, padding?: number[]): void;
    destroy(): void;
  }
  class LngLat {
    constructor(lng: number, lat: number);
  }
  class Marker {
    constructor(opts?: Record<string, unknown>);
    getPosition(): LngLat | undefined;
    on(event: string, handler: () => void): void;
  }
  class InfoWindow {
    constructor(opts?: Record<string, unknown>);
    setContent(content: string): void;
    open(map: Map, position: LngLat): void;
    close(): void;
  }
  class Bounds {
    constructor(southWest: LngLat, northEast: LngLat);
    extend(point: LngLat): void;
  }
  class Pixel {
    constructor(x: number, y: number);
  }
}
