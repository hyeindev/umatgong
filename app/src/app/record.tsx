import { useRouter } from 'expo-router';
import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { BackButton } from '@/components/BackButton';
import { useMyClubs } from '@/features/club';
import { SOUTH_KOREA_BOUNDS, useDebouncedValue } from '@/features/map';
import {
  CandidateList,
  ClubChoice,
  PhotoAttachments,
  RatingPicker,
  annotate,
  rankNearby,
  recordErrorMessage,
  useNearbyPlaces,
  useNearbyVisits,
  useSaveVisit,
  useSearchPlaces,
  type PlaceChoice,
} from '@/features/record';
import { location } from '@/lib/location';
import { Map, type MapHandle, type MapMarker } from '@/lib/map';
import { colors, layout, shape, size, spacing, textStyles } from '@/theme';
import type { Club } from '@/types/club';
import type { Coordinate } from '@/types/geo';
import type { Rating } from '@/types/visit';

// docs/api-spec.md: 메모 200자, 가게 이름 100자
const MAX_MEMO_LENGTH = 200;
const MAX_PLACE_NAME_LENGTH = 100;
// 후보는 하단 절반 안에서 스크롤 없이 대부분 보이는 만큼만
const MAX_CANDIDATES = 8;
const SEARCH_DEBOUNCE_MS = 300;

// 기록하기 — 수동 경로 (화면기획서 4.5·4.6). 웹에서도 동작한다. 사진첩 자동 스캔은 네이티브 전용이라 여기 없다.
// ① 장소 고르기(주변 후보 / 가게명 검색 / 지도 찍기 / 「여기 없어요」 직접 입력) → ② 평가·메모·사진·클럽 → 저장.
// 후보 목록과 평가 버튼은 화면 하단 절반에 둔다 (한 손 조작).
export default function RecordScreen() {
  const [choice, setChoice] = useState<PlaceChoice | null>(null);
  return choice ? (
    <DetailStep choice={choice} onChangePlace={() => setChoice(null)} />
  ) : (
    <PlaceStep onChoose={setChoice} />
  );
}

// ── ① 장소 고르기 ──────────────────────────────────────────────────────────

type Mode = 'nearby' | 'search' | 'new';

function PlaceStep({ onChoose }: { onChoose: (choice: PlaceChoice) => void }) {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const mapRef = useRef<MapHandle>(null);
  const clubs = useMyClubs().data ?? [];
  const [myLocation, setMyLocation] = useState<Coordinate | null>(null);
  const [picked, setPicked] = useState<Coordinate | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [mode, setMode] = useState<Mode>('nearby');
  const [query, setQuery] = useState('');
  const [newName, setNewName] = useState('');
  const [newAddress, setNewAddress] = useState('');

  // 지도에서 찍은 곳이 있으면 그곳, 없으면 지금 내 위치가 후보의 기준이다
  const center = picked ?? myLocation;

  useEffect(() => {
    let active = true;
    location.getCurrent().then((result) => {
      if (!active) {
        return;
      }
      if (result.status === 'granted') {
        setMyLocation(result.coordinate);
        mapRef.current?.moveTo(result.coordinate, 'neighborhood');
      } else {
        setNotice('위치를 알 수 없어요. 지도를 눌러 먹은 곳을 찍거나 가게명으로 찾아 주세요.');
      }
    });
    return () => {
      active = false;
    };
  }, []);

  const nearbyPlaces = useNearbyPlaces(center);
  const nearbyVisits = useNearbyVisits(center);
  const debouncedQuery = useDebouncedValue(query, SEARCH_DEBOUNCE_MS);
  const search = useSearchPlaces(mode === 'search' ? debouncedQuery : '', center);

  const nearbyCandidates = useMemo(
    () => rankNearby(nearbyPlaces.data ?? [], nearbyVisits.data ?? []).slice(0, MAX_CANDIDATES),
    [nearbyPlaces.data, nearbyVisits.data],
  );
  const searchCandidates = useMemo(
    () => annotate(search.data ?? [], nearbyVisits.data ?? []),
    [search.data, nearbyVisits.data],
  );

  const markers = useMemo<MapMarker[]>(
    () =>
      picked
        ? [
            {
              kind: 'pin',
              id: 'picked',
              coordinate: picked,
              shape: 'droplet',
              color: colors.surface.cream,
              label: '고른 위치',
            },
          ]
        : [],
    [picked],
  );

  const onMapPress = useCallback((coordinate: Coordinate) => {
    setPicked(coordinate);
    setNotice(null);
    setMode((current) => (current === 'search' ? 'nearby' : current));
  }, []);

  const notHere = (
    <Pressable
      accessibilityRole="button"
      onPress={() => {
        setNewName(mode === 'search' ? query.trim() : '');
        setMode('new');
      }}
      style={({ pressed }) => [styles.notHere, pressed && styles.pressed]}
    >
      <Text style={styles.notHereLabel}>여기 없어요 · 직접 입력</Text>
    </Pressable>
  );

  return (
    <View style={styles.screen}>
      <View style={styles.mapArea}>
        <Map
          ref={mapRef}
          initialBounds={SOUTH_KOREA_BOUNDS}
          restrictTo={SOUTH_KOREA_BOUNDS}
          myLocation={myLocation}
          markers={markers}
          onPress={onMapPress}
          onError={setNotice}
          style={StyleSheet.absoluteFill}
        />
        <View
          style={[styles.mapHeader, { paddingTop: insets.top + spacing[3] }]}
          pointerEvents="box-none"
        >
          <BackButton label="지도" fallback="/" />
          <Text style={styles.title} accessibilityRole="header">
            어디서 먹었어요?
          </Text>
          <Text style={styles.mapHint}>
            {notice ??
              (picked ? '찍은 곳 근처 가게를 보여 드려요' : '지도를 누르면 그 근처 가게를 찾아요')}
          </Text>
        </View>
      </View>

      <KeyboardAvoidingView behavior="padding" style={styles.panel}>
        <View style={styles.tabs} accessibilityRole="tablist">
          <Tab label="주변" active={mode === 'nearby'} onPress={() => setMode('nearby')} />
          <Tab label="가게명 검색" active={mode === 'search'} onPress={() => setMode('search')} />
        </View>

        <ScrollView
          contentContainerStyle={[
            styles.panelContent,
            { paddingBottom: insets.bottom + spacing[6] },
          ]}
          keyboardShouldPersistTaps="handled"
        >
          {mode === 'nearby' ? (
            <CandidateList
              candidates={nearbyCandidates}
              loading={center !== null && (nearbyPlaces.isPending || nearbyVisits.isPending)}
              error={nearbyPlaces.isError ? recordErrorMessage(nearbyPlaces.error) : null}
              emptyText={
                center
                  ? '근처에 찾은 가게가 없어요.'
                  : '지도를 눌러 위치를 정하거나 가게명으로 찾아 주세요.'
              }
              onSelect={(candidate) => onChoose({ kind: 'place', place: candidate.place })}
              footer={notHere}
            />
          ) : null}

          {mode === 'search' ? (
            <View style={styles.searchWrap}>
              <TextInput
                value={query}
                onChangeText={setQuery}
                placeholder="가게 이름"
                placeholderTextColor={colors.text.tertiary}
                autoFocus
                returnKeyType="search"
                accessibilityLabel="가게 이름 검색"
                style={styles.input}
              />
              <CandidateList
                candidates={searchCandidates}
                loading={debouncedQuery.trim().length > 0 && search.isPending}
                error={search.isError ? recordErrorMessage(search.error) : null}
                emptyText={
                  debouncedQuery.trim() ? '찾는 가게가 없어요.' : '가게 이름을 적어 주세요.'
                }
                onSelect={(candidate) => onChoose({ kind: 'place', place: candidate.place })}
                footer={notHere}
              />
            </View>
          ) : null}

          {mode === 'new' ? (
            <NewPlaceForm
              clubs={clubs}
              coordinate={center}
              usingPickedPoint={picked !== null}
              name={newName}
              address={newAddress}
              onChangeName={setNewName}
              onChangeAddress={setNewAddress}
              onCancel={() => setMode('nearby')}
              onGoToClubs={() => router.push('/clubs')}
              onSubmit={(coordinate) =>
                onChoose({
                  kind: 'new',
                  name: newName.trim(),
                  address: newAddress.trim() || null,
                  coordinate,
                })
              }
            />
          ) : null}
        </ScrollView>
      </KeyboardAvoidingView>
    </View>
  );
}

function Tab({ label, active, onPress }: { label: string; active: boolean; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="tab"
      accessibilityState={{ selected: active }}
      onPress={onPress}
      style={({ pressed }) => [styles.tab, active && styles.tabActive, pressed && styles.pressed]}
    >
      <Text style={[styles.tabLabel, active && styles.tabLabelActive]}>{label}</Text>
    </Pressable>
  );
}

type NewPlaceFormProps = {
  clubs: readonly Club[];
  coordinate: Coordinate | null;
  usingPickedPoint: boolean;
  name: string;
  address: string;
  onChangeName: (value: string) => void;
  onChangeAddress: (value: string) => void;
  onCancel: () => void;
  onGoToClubs: () => void;
  onSubmit: (coordinate: Coordinate) => void;
};

/**
 * 「여기 없어요」— 가게명 직접 입력 (화면기획서 4.6). 클럽 전용 장소로 등록되므로 클럽이 있어야 한다.
 * 위치는 지도에서 찍은 곳, 없으면 지금 내 위치다.
 */
function NewPlaceForm({
  clubs,
  coordinate,
  usingPickedPoint,
  name,
  address,
  onChangeName,
  onChangeAddress,
  onCancel,
  onGoToClubs,
  onSubmit,
}: NewPlaceFormProps) {
  if (clubs.length === 0) {
    return (
      <View style={styles.form}>
        <Text style={styles.formTitle}>직접 등록은 클럽이 있어야 해요</Text>
        <Text style={styles.body}>
          직접 적은 가게는 우리 클럽 안에서만 보여요. 클럽을 만들거나 초대받은 뒤 등록해 주세요.
        </Text>
        <SecondaryButton label="클럽 만들기" onPress={onGoToClubs} />
        <SecondaryButton label="후보로 돌아가기" onPress={onCancel} />
      </View>
    );
  }
  const canSubmit = name.trim().length > 0 && coordinate !== null;
  return (
    <View style={styles.form}>
      <Text style={styles.formTitle}>가게를 직접 적어 주세요</Text>
      <TextInput
        value={name}
        onChangeText={onChangeName}
        placeholder="가게 이름"
        placeholderTextColor={colors.text.tertiary}
        maxLength={MAX_PLACE_NAME_LENGTH}
        autoFocus
        accessibilityLabel="가게 이름"
        style={styles.input}
      />
      <TextInput
        value={address}
        onChangeText={onChangeAddress}
        placeholder="주소 (선택)"
        placeholderTextColor={colors.text.tertiary}
        accessibilityLabel="주소"
        style={styles.input}
      />
      <Text style={styles.body}>
        {coordinate
          ? usingPickedPoint
            ? '위치: 지도에서 찍은 곳'
            : '위치: 지금 내 위치 · 다른 곳이면 지도를 눌러 주세요'
          : '지도를 눌러 가게 위치를 찍어 주세요.'}
      </Text>
      <Text style={styles.caption}>
        우리 클럽 친구에게만 보여요. 다음에 이 근처에서 후보로 떠요.
      </Text>
      <Pressable
        accessibilityRole="button"
        accessibilityState={{ disabled: !canSubmit }}
        disabled={!canSubmit}
        onPress={() => coordinate && onSubmit(coordinate)}
        style={({ pressed }) => [
          styles.primary,
          !canSubmit && styles.primaryDisabled,
          pressed && styles.pressed,
        ]}
      >
        <Text style={[styles.primaryLabel, !canSubmit && styles.primaryLabelDisabled]}>
          이 가게로 기록하기
        </Text>
      </Pressable>
      <SecondaryButton label="후보로 돌아가기" onPress={onCancel} />
    </View>
  );
}

// ── ② 평가·메모·사진·클럽 ───────────────────────────────────────────────────

function DetailStep({ choice, onChangePlace }: { choice: PlaceChoice; onChangePlace: () => void }) {
  const insets = useSafeAreaInsets();
  const router = useRouter();
  const clubsQuery = useMyClubs();
  const clubs = useMemo(() => clubsQuery.data ?? [], [clubsQuery.data]);
  const [rating, setRating] = useState<Rating | null>(null);
  const [memo, setMemo] = useState('');
  const [photos, setPhotos] = useState<{ urls: string[]; uploading: boolean }>({
    urls: [],
    uploading: false,
  });
  const [pickedClubId, setPickedClubId] = useState<number | null>(null);
  const save = useSaveVisit();

  // 클럽 전용 장소는 그 클럽으로만 기록할 수 있다. 클럽이 하나면 묻지 않는다. 없으면 나만 보는 기록
  const lockedClub =
    choice.kind === 'place' && choice.place.clubId !== null
      ? (clubs.find((club) => club.id === choice.place.clubId) ?? null)
      : null;
  const clubId = lockedClub?.id ?? (clubs.length === 1 ? (clubs[0]?.id ?? null) : pickedClubId);
  const mustPickClub = lockedClub === null && clubs.length >= 2;

  const name = choice.kind === 'place' ? choice.place.name : choice.name;
  const detail =
    choice.kind === 'place'
      ? [choice.place.category, choice.place.address].filter(Boolean).join(' · ')
      : '직접 등록하는 가게 · 우리 클럽에만 보여요';
  const coordinate = choice.kind === 'place' ? choice.place.coordinate : choice.coordinate;

  const missing = !rating
    ? '평가를 골라 주세요'
    : mustPickClub && clubId === null
      ? '클럽을 골라 주세요'
      : photos.uploading
        ? '사진을 올리는 중이에요'
        : null;
  const canSave = missing === null && !save.isPending && !clubsQuery.isPending;

  const submit = () => {
    if (!canSave || !rating) {
      return;
    }
    save.mutate(
      {
        choice,
        clubId,
        request: {
          rating,
          memo: memo.trim() || undefined,
          visitedAt: new Date().toISOString(),
          thumbnailUrls: photos.urls.length > 0 ? photos.urls : undefined,
        },
      },
      {
        // 지도로 돌아가 새 핀이 있는 곳으로 옮긴다 (화면기획서 5장 “기록 완료 → 새 핀이 찍히는 것을 보여줌”)
        onSuccess: () => {
          const params = {
            focusLat: String(coordinate.lat),
            focusLng: String(coordinate.lng),
            recorded: String(Date.now()),
          };
          if (router.canDismiss()) {
            router.dismissTo({ pathname: '/', params });
          } else {
            router.replace({ pathname: '/', params });
          }
        },
      },
    );
  };

  return (
    <KeyboardAvoidingView behavior="padding" style={styles.screen}>
      <ScrollView
        contentContainerStyle={[styles.detailTop, { paddingTop: insets.top + spacing[3] }]}
        keyboardShouldPersistTaps="handled"
      >
        <Pressable
          accessibilityRole="button"
          onPress={onChangePlace}
          style={({ pressed }) => [styles.back, pressed && styles.pressed]}
        >
          <Text style={styles.backLabel}>다른 곳 고르기</Text>
        </Pressable>
        <View style={styles.placeCard}>
          <Text style={styles.placeName} accessibilityRole="header">
            {name}
          </Text>
          {detail ? <Text style={styles.body}>{detail}</Text> : null}
        </View>
        <TextInput
          value={memo}
          onChangeText={setMemo}
          placeholder="한 줄 메모 (선택)"
          placeholderTextColor={colors.text.tertiary}
          maxLength={MAX_MEMO_LENGTH}
          accessibilityLabel="한 줄 메모"
          style={styles.input}
        />
        <PhotoAttachments onChange={setPhotos} />
      </ScrollView>

      <View style={[styles.detailBottom, { paddingBottom: insets.bottom + spacing[5] }]}>
        {mustPickClub ? (
          <ClubChoice clubs={clubs} value={clubId} onChange={setPickedClubId} />
        ) : lockedClub ? (
          <Text style={styles.caption}>「{lockedClub.name}」 전용 가게라 이 클럽에 남겨요.</Text>
        ) : clubs.length === 0 && !clubsQuery.isPending ? (
          <Text style={styles.caption}>클럽이 없어서 나만 보는 기록으로 남아요.</Text>
        ) : null}
        <RatingPicker value={rating} onChange={setRating} />
        {save.isError ? (
          <Text style={styles.error} accessibilityRole="alert">
            {recordErrorMessage(save.error)}
          </Text>
        ) : null}
        <Pressable
          accessibilityRole="button"
          accessibilityState={{ disabled: !canSave, busy: save.isPending }}
          accessibilityHint={missing ?? undefined}
          disabled={!canSave}
          onPress={submit}
          style={({ pressed }) => [
            styles.primary,
            !canSave && styles.primaryDisabled,
            pressed && styles.pressed,
          ]}
        >
          {save.isPending ? (
            <ActivityIndicator color={colors.accent.on} />
          ) : (
            <Text style={[styles.primaryLabel, !canSave && styles.primaryLabelDisabled]}>
              {missing ?? '기록하기'}
            </Text>
          )}
        </Pressable>
      </View>
    </KeyboardAvoidingView>
  );
}

function SecondaryButton({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      onPress={onPress}
      style={({ pressed }) => [styles.secondary, pressed && styles.pressed]}
    >
      <Text style={styles.secondaryLabel}>{label}</Text>
    </Pressable>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background.screen,
  },
  // 위쪽 45%는 지도, 아래 55%는 후보 — 후보를 누르는 손가락이 화면 하단 절반에 머문다
  mapArea: {
    flex: 45,
  },
  mapHeader: {
    paddingHorizontal: layout.screenGutter,
    gap: spacing[2.5],
  },
  title: {
    ...textStyles.title,
    color: colors.text.primary,
  },
  mapHint: {
    ...textStyles.bodySmall,
    color: colors.text.primary,
    alignSelf: 'flex-start',
    paddingVertical: spacing[1.5],
    paddingHorizontal: spacing[3],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.floating,
    overflow: 'hidden',
  },
  panel: {
    flex: 55,
    borderTopLeftRadius: shape.sheet,
    borderTopRightRadius: shape.sheet,
    backgroundColor: colors.surface.sheet,
    paddingTop: spacing[4],
  },
  tabs: {
    flexDirection: 'row',
    gap: spacing[1.5],
    paddingHorizontal: layout.screenGutter,
    paddingBottom: spacing[3],
  },
  tab: {
    paddingVertical: spacing[2],
    paddingHorizontal: spacing[3.5],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.raised,
  },
  tabActive: {
    backgroundColor: colors.surface.cream,
  },
  tabLabel: {
    ...textStyles.label,
    color: colors.text.primary,
  },
  tabLabelActive: {
    color: colors.text.onCream.primary,
  },
  panelContent: {
    paddingHorizontal: layout.screenGutter,
    gap: spacing[3],
  },
  searchWrap: {
    gap: spacing[3],
  },
  input: {
    ...textStyles.body,
    color: colors.text.primary,
    height: size.button,
    paddingHorizontal: spacing[4],
    borderRadius: shape.button,
    backgroundColor: colors.background.screen,
    borderWidth: 1,
    borderColor: colors.border.strong,
  },
  notHere: {
    alignSelf: 'center',
    paddingVertical: spacing[3],
    paddingHorizontal: spacing[4],
  },
  notHereLabel: {
    ...textStyles.label,
    color: colors.text.secondary,
    textDecorationLine: 'underline',
  },
  form: {
    gap: spacing[3],
  },
  formTitle: {
    ...textStyles.itemTitle,
    color: colors.text.primary,
  },
  body: {
    ...textStyles.bodySmall,
    color: colors.text.secondary,
  },
  caption: {
    ...textStyles.bodySmall,
    color: colors.text.tertiary,
  },
  error: {
    ...textStyles.bodySmall,
    color: colors.text.primary,
  },
  primary: {
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.accent.default,
    alignItems: 'center',
    justifyContent: 'center',
  },
  primaryDisabled: {
    backgroundColor: colors.surface.raised,
  },
  primaryLabel: {
    ...textStyles.button,
    color: colors.accent.on,
  },
  primaryLabelDisabled: {
    color: colors.text.tertiary,
  },
  secondary: {
    height: size.button,
    borderRadius: shape.button,
    backgroundColor: colors.surface.raised,
    alignItems: 'center',
    justifyContent: 'center',
  },
  secondaryLabel: {
    ...textStyles.button,
    color: colors.text.primary,
  },
  pressed: {
    opacity: 0.85,
  },
  detailTop: {
    flexGrow: 1,
    paddingHorizontal: layout.screenGutter,
    paddingBottom: spacing[5],
    gap: spacing[4],
  },
  back: {
    alignSelf: 'flex-start',
    paddingVertical: spacing[2.5],
    paddingHorizontal: spacing[3.5],
    borderRadius: shape.pill,
    backgroundColor: colors.surface.raised,
  },
  backLabel: {
    ...textStyles.label,
    color: colors.text.primary,
  },
  placeCard: {
    gap: spacing[1.5],
  },
  placeName: {
    ...textStyles.display,
    color: colors.text.primary,
  },
  // 평가·클럽·저장은 화면 아래에 붙여 둔다 (한 손 조작)
  detailBottom: {
    paddingTop: spacing[4],
    paddingHorizontal: layout.screenGutter,
    gap: spacing[3],
    borderTopLeftRadius: shape.sheet,
    borderTopRightRadius: shape.sheet,
    backgroundColor: colors.surface.sheet,
  },
});
