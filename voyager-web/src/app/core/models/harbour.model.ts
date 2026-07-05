export interface Harbour {
  id: number;
  name: string;
  lat: number;
  lon: number;
  vhfChannel: string | null;
  phone: string | null;
  depthM: number | null;
  priceNote: string | null;
  hasHost: boolean;
  harbourCode: string | null;
  nickname: string | null;
  address: string | null;
  website: string | null;
  email: string | null;
  amenities: string | null;
  sourceUrl: string | null;
}

/** Matches ee.voyagelog.api.GeoJson.FeatureCollection on the backend. */
export interface HarbourFeatureCollection {
  type: 'FeatureCollection';
  features: HarbourFeature[];
}

export interface HarbourFeature {
  type: 'Feature';
  geometry: { type: 'Point'; coordinates: [number, number] };
  properties: {
    id: number;
    name: string;
    vhfChannel: string | null;
    phone: string | null;
    depthM: number | null;
    priceNote: string | null;
    hasHost: boolean;
    harbourCode: string | null;
    nickname: string | null;
    address: string | null;
    website: string | null;
    email: string | null;
    amenities: string | null;
    sourceUrl: string | null;
  };
}

export function harbourFromFeature(f: HarbourFeature): Harbour {
  const [lon, lat] = f.geometry.coordinates;
  return {
    id: f.properties.id,
    name: f.properties.name,
    lat,
    lon,
    vhfChannel: f.properties.vhfChannel,
    phone: f.properties.phone,
    depthM: f.properties.depthM,
    priceNote: f.properties.priceNote,
    hasHost: f.properties.hasHost,
    harbourCode: f.properties.harbourCode,
    nickname: f.properties.nickname,
    address: f.properties.address,
    website: f.properties.website,
    email: f.properties.email,
    amenities: f.properties.amenities,
    sourceUrl: f.properties.sourceUrl,
  };
}
