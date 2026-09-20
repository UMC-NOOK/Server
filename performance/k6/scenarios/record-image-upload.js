import { check } from "k6";
import http from "k6/http";
import { Rate, Trend } from "k6/metrics";

import { recordImageUploadOptions } from "../config/profiles.js";
import { authenticateDevUser } from "../lib/auth.js";
import { createUserBook } from "../lib/books.js";
import { requireApiResponse } from "../lib/checks.js";
import { recordContent, scenarioUser } from "../lib/data.js";
import { authHeaders, post } from "../lib/http.js";
import { imageContentType, sampleImageBytes, uploadUrlRequestPayload } from "../lib/images.js";
import { intEnv, stringEnv } from "../lib/env.js";
import { createSummary } from "../lib/summary.js";

export const options = recordImageUploadOptions;

const journeyDuration = new Trend("record_upload_journey_duration", true);
const journeySuccess = new Rate("record_upload_journey_success");

function checkPutImage(putImage, target) {
  check(putImage, {
    "record upload: image PUT succeeded": (res) => res.status === 200,
  });
  if (putImage.status !== 200) {
    const safeUrl = target.imageUrl.split("?")[0];
    console.error(
      `record upload: PUT failed key=${target.key} url=${safeUrl} status=${putImage.status} ` +
      `error=${putImage.error} error_code=${putImage.error_code} body=${(putImage.body || "").slice(0, 500)}`
    );
  }
}

export function setup() {
  const auth = authenticateDevUser(scenarioUser("record-upload"));
  return { accessToken: auth.accessToken };
}

export default function (setupData) {
  const params = {
    headers: authHeaders(setupData.accessToken),
  };
  const imageCount = intEnv("IMAGE_COUNT", 3);
  const contentLength = intEnv("CONTENT_LENGTH", 50);

  // 각 VU가 자기 책을 따로 만들어야 실제 "서로 다른 사람이 각자 기록을 쓰는" 상황과 같아집니다.
  // 책(library)을 공유하면 RecordCommandService의 비관적 락(FOR UPDATE)이 VU들을 한 줄로 세워버려
  // 커넥션 풀 부족 외에 락 경합까지 섞여서 순수한 풀 영향만 재기 어렵습니다.
  const book = createUserBook(params, {}, "record-upload:create-book");

  const journeyStart = Date.now();

  try {
    const issueUrls = post("/api/v1/images/upload-urls", uploadUrlRequestPayload(imageCount), {
      ...params,
      tags: { name: "record-upload:issue-urls" },
    });
    if (issueUrls.status !== 200) {
      console.error(`record upload: issue-urls failed status=${issueUrls.status} body=${(issueUrls.body || "").slice(0, 500)}`);
    }
    requireApiResponse(issueUrls, {
      label: "record upload: issue upload urls",
      statuses: [200],
      requireResult: true,
    });

    const targets = issueUrls.json("result");
    check(issueUrls, {
      "record upload: received an upload target per image": () => Array.isArray(targets) && targets.length === imageCount,
    });

    const imageBytes = sampleImageBytes();
    const imageKeys = targets.map((target) => target.key);

    // PUT_MODE=parallel은 클라이언트가 이미지를 동시에 올릴 때, sequential은 한 장씩 순서대로 올릴 때를 흉내냅니다.
    // 어느 쪽이 실제 앱의 "5장이면 오래 걸림" 체감을 만드는지 구분해서 재기 위한 스위치입니다.
    if (stringEnv("PUT_MODE", "sequential") === "parallel") {
      const requests = targets.map((target) => ({
        method: "PUT",
        url: target.imageUrl,
        body: imageBytes,
        params: {
          headers: { "Content-Type": imageContentType() },
          tags: { name: "record-upload:put-image" },
        },
      }));
      const responses = http.batch(requests);
      responses.forEach((putImage, index) => checkPutImage(putImage, targets[index]));
    } else {
      for (const target of targets) {
        const putImage = http.put(target.imageUrl, imageBytes, {
          headers: { "Content-Type": imageContentType() },
          tags: { name: "record-upload:put-image" },
        });
        checkPutImage(putImage, target);
      }
    }

    const createRecord = post(`/api/v1/records/books/${book.bookId}`, {
      content: recordContent(contentLength),
      emotion: "FUN",
      imageKeys,
    }, {
      ...params,
      tags: { name: "record-upload:create-record" },
    });
    if (createRecord.status !== 201 && createRecord.status !== 200) {
      console.error(`record upload: create-record failed status=${createRecord.status} body=${(createRecord.body || "").slice(0, 500)}`);
    }
    requireApiResponse(createRecord, {
      label: "record upload: create record",
      statuses: [201, 200],
      requireResult: true,
    });

    journeySuccess.add(true);
  } catch (error) {
    journeySuccess.add(false);
    throw error;
  } finally {
    journeyDuration.add(Date.now() - journeyStart);
  }
}

export function handleSummary(data) {
  return createSummary(data);
}
