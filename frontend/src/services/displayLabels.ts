const workspaceStatusLabels: Record<string, string> = {
  APPROVED: '已收录',
  CANDIDATE: '待完善',
  PENDING: '待审核',
  REJECTED: '未收录'
};

const trustLevelLabels: Record<string, string> = {
  LOW: '资料较少',
  MEDIUM: '资料适中',
  HIGH: '资料充分'
};

const crowdLevelLabels: Record<string, string> = {
  FREE: '空闲',
  NORMAL: '适中',
  CROWDED: '较拥挤',
  FULL: '爆满'
};

const noiseLevelLabels: Record<string, string> = {
  QUIET: '安静',
  RELATIVELY_QUIET: '较安静',
  NORMAL: '一般',
  NOISY: '较吵',
  VERY_NOISY: '很吵'
};

const sceneLabels: Record<string, string> = {
  SELF_STUDY: '自习',
  READING: '阅读',
  REMOTE_WORK: '远程办公',
  TEMPORARY_WORK: '临时办公',
  GROUP_DISCUSSION: '小组讨论',
  VIDEO_MEETING: '视频会议'
};

const auditStatusLabels: Record<string, string> = {
  PENDING: '审核中',
  APPROVED: '已通过',
  REJECTED: '已拒绝',
  DELETED: '已删除'
};

const allowLongStayLabels: Record<string, string> = {
  TRUE: '适合久坐',
  FALSE: '不适合久坐',
  UNKNOWN: '不确定'
};

function labelFrom(map: Record<string, string>, value: string | null | undefined, fallback: string): string {
  if (!value) return fallback;
  return map[value] ?? value;
}

export function workspaceStatusLabel(value: string | null | undefined): string {
  return labelFrom(workspaceStatusLabels, value, '候选');
}

export function trustLevelLabel(value: string | null | undefined): string {
  return labelFrom(trustLevelLabels, value, '资料较少');
}

export function crowdLevelLabel(value: string | null | undefined): string {
  return labelFrom(crowdLevelLabels, value, '-');
}

export function noiseLevelLabel(value: string | null | undefined): string {
  return labelFrom(noiseLevelLabels, value, '-');
}

export function sceneLabel(value: string | null | undefined): string {
  return labelFrom(sceneLabels, value, '');
}

export function auditStatusLabel(value: string | null | undefined): string {
  return labelFrom(auditStatusLabels, value, '');
}

export function allowLongStayLabel(value: string | null | undefined): string {
  return labelFrom(allowLongStayLabels, value, '');
}
