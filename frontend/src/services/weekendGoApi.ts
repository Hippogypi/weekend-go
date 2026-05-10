import { ApiClient } from './apiClient';
import type { AccessTokenProvider } from './apiClient';

export interface WeekendGoApiOptions {
  baseUrl: string;
  accessTokenProvider?: AccessTokenProvider;
  fetcher?: typeof fetch;
}

export interface AuthLoginRequest {
  username: string;
  password: string;
}

export interface AuthRegisterRequest extends AuthLoginRequest {
  nickname: string;
}

export interface UserProfile {
  id: number;
  username: string;
  role: 'USER' | 'ADMIN' | string;
  nickname: string;
}

export interface AuthTokenResponse {
  token: string;
  user: UserProfile;
}

export interface WorkspaceProfile {
  placeId: number;
  quietScore: number;
  wifiScore: number;
  socketScore: number;
  seatScore: number;
  costScore?: number | null;
  minConsumption?: number | null;
  allowLongStay?: string | null;
  score: number;
  trustLevel: string;
  approvedSubmissionCount: number;
  contributorCount: number;
  lastContributedAt?: string | null;
}

export interface Place {
  id: number;
  amapPoiId?: string | null;
  name: string;
  address?: string | null;
  longitude?: number | string | null;
  latitude?: number | string | null;
  amapType?: string | null;
  amapTypeCode?: string | null;
  province?: string | null;
  city?: string | null;
  district?: string | null;
  source?: string | null;
  workspaceStatus?: string | null;
  workspaceProfile?: WorkspaceProfile | null;
}

export interface SearchPlacesParams {
  keyword: string;
  city?: string;
  page?: number;
  offset?: number;
}

export interface NearbyPlacesParams {
  longitude: string;
  latitude: string;
  keyword?: string;
  radius?: number;
  page?: number;
  offset?: number;
}

export interface ProfileSubmissionRequest {
  quietScore: number;
  wifiScore: number;
  socketScore: number;
  seatScore: number;
  costScore?: number | null;
  minConsumption?: number | null;
  allowLongStay?: string | null;
  suitableScenes?: string[];
  remark?: string;
}

export interface ProfileSubmission extends ProfileSubmissionRequest {
  id: number;
  placeId: number;
  userId: number;
  auditStatus: string;
  auditReason?: string | null;
  createdAt?: string;
}

export interface CheckinRequest {
  crowdLevel: 'QUIET' | 'NORMAL' | 'CROWDED' | string;
  noiseLevel: 'QUIET' | 'RELATIVELY_QUIET' | 'NORMAL' | 'NOISY' | string;
  hasSeat: boolean;
  remark?: string;
}

export interface CurrentStatus {
  placeId: number;
  status: string;
  message: string;
  sampleCount: number;
  since?: string;
  crowdLevel?: string | null;
  noiseLevel?: string | null;
  hasSeat?: boolean | null;
  seatAvailabilityRatio?: number | null;
}

export interface ProfileAttributeRequest {
  minConsumption?: number | null;
  allowLongStay?: string | null;
  suitableScenes?: string[];
}

export interface ReviewImageAttachment {
  imageUrl: string;
  description?: string;
}

export interface ReviewRequest {
  quietScore: number;
  wifiScore: number;
  socketScore: number;
  comfortScore: number;
  costScore: number;
  content: string;
  profileAttributes?: ProfileAttributeRequest | null;
  images?: ReviewImageAttachment[];
}

export interface Review extends ReviewRequest {
  id: number;
  placeId: number;
  userId: number;
  auditStatus?: string | null;
  createdAt?: string;
  images?: PlaceImage[];
}

export interface ImageRequest {
  imageUrl: string;
  description?: string;
}

export interface PlaceImage extends ImageRequest {
  id: number;
  placeId: number;
  userId: number;
  auditStatus?: string | null;
  createdAt?: string;
}

export interface FavoriteStatus {
  placeId: number;
  favorited: boolean;
}

export interface FavoritePlace {
  placeId: number;
  placeName: string;
  createdAt: string;
}

export interface AuditRequest {
  auditStatus: 'APPROVED' | 'REJECTED';
  reason?: string;
}

export class WeekendGoApi {
  private readonly client: ApiClient;

  constructor(options: WeekendGoApiOptions) {
    this.client = new ApiClient(options);
  }

  register(body: AuthRegisterRequest): Promise<UserProfile> {
    return this.client.post('/auth/register', body);
  }

  login(body: AuthLoginRequest): Promise<AuthTokenResponse> {
    return this.client.post('/auth/login', body);
  }

  me(): Promise<UserProfile> {
    return this.client.get('/auth/me');
  }

  logout(): Promise<void> {
    return this.client.post('/auth/logout', {});
  }

  searchPlaces(params: SearchPlacesParams): Promise<Place[]> {
    const query = new URLSearchParams();
    query.set('keyword', params.keyword);
    if (params.city) {
      query.set('city', params.city);
    }
    query.set('page', String(params.page ?? 1));
    query.set('offset', String(params.offset ?? 10));

    return this.client.get(`/workspaces/search?${query.toString()}`);
  }

  nearbyPlaces(params: NearbyPlacesParams): Promise<Place[]> {
    const query = new URLSearchParams();
    query.set('longitude', params.longitude);
    query.set('latitude', params.latitude);
    if (params.keyword) {
      query.set('keyword', params.keyword);
    }
    query.set('radius', String(params.radius ?? 1000));
    query.set('page', String(params.page ?? 1));
    query.set('offset', String(params.offset ?? 10));

    return this.client.get(`/workspaces/nearby?${query.toString()}`);
  }

  placeDetail(placeId: number | string): Promise<Place> {
    return this.client.get(`/places/${placeId}`);
  }

  workspaceProfile(placeId: number | string): Promise<WorkspaceProfile> {
    return this.client.get(`/places/${placeId}/workspace-profile`);
  }

  submitProfile(placeId: number | string, body: ProfileSubmissionRequest): Promise<ProfileSubmission> {
    return this.client.post(`/places/${placeId}/profile-submissions`, body);
  }

  approveProfileSubmission(submissionId: number | string, reason?: string): Promise<ProfileSubmission> {
    return this.client.post(`/admin/profile-submissions/${submissionId}/approve`, { reason });
  }

  rejectProfileSubmission(submissionId: number | string, reason: string): Promise<ProfileSubmission> {
    return this.client.post(`/admin/profile-submissions/${submissionId}/reject`, { reason });
  }

  submitCheckin(placeId: number | string, body: CheckinRequest): Promise<unknown> {
    return this.client.post(`/places/${placeId}/checkins`, body);
  }

  currentStatus(placeId: number | string): Promise<CurrentStatus> {
    return this.client.get(`/places/${placeId}/current-status`);
  }

  submitReview(placeId: number | string, body: ReviewRequest): Promise<Review> {
    return this.client.post(`/places/${placeId}/reviews`, body);
  }

  reviews(placeId: number | string): Promise<Review[]> {
    return this.client.get(`/places/${placeId}/reviews`);
  }

  submitImage(placeId: number | string, body: ImageRequest): Promise<PlaceImage> {
    return this.client.post(`/places/${placeId}/images`, body);
  }

  images(placeId: number | string): Promise<PlaceImage[]> {
    return this.client.get(`/places/${placeId}/images`);
  }

  favoriteStatus(placeId: number | string): Promise<FavoriteStatus> {
    return this.client.get(`/places/${placeId}/favorite`);
  }

  addFavorite(placeId: number | string): Promise<FavoriteStatus> {
    return this.client.post(`/places/${placeId}/favorite`, {});
  }

  removeFavorite(placeId: number | string): Promise<FavoriteStatus> {
    return this.client.delete(`/places/${placeId}/favorite`);
  }

  favorites(): Promise<FavoritePlace[]> {
    return this.client.get('/me/favorites');
  }

  auditReview(reviewId: number | string, body: AuditRequest): Promise<Review> {
    return this.client.patch(`/admin/reviews/${reviewId}/audit`, body);
  }

  auditImage(imageId: number | string, body: AuditRequest): Promise<PlaceImage> {
    return this.client.patch(`/admin/images/${imageId}/audit`, body);
  }
}

export function createWeekendGoApi(options: WeekendGoApiOptions): WeekendGoApi {
  return new WeekendGoApi(options);
}
