export function timelineItems(timelinePreview) {
  const dateGroups = timelinePreview?.dateGroups || [];
  const items = [];

  dateGroups.forEach((group) => {
    (group.items || []).forEach((item) => items.push(item));
  });

  return items;
}

export function findTimelineItemByType(timelinePreview, type) {
  return timelineItems(timelinePreview).find((item) => item.type === type);
}
